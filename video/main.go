package main

import (
	"contrib.go.opencensus.io/exporter/jaeger"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	uberCompat "github.com/nkonev/jaeger-uber-propagation-compat/propagation"
	log "github.com/pion/ion-log"
	"github.com/pion/ion-sfu/cmd/signal/json-rpc/server"
	"github.com/pion/ion-sfu/pkg/middlewares/datachannel"
	"github.com/pion/ion-sfu/pkg/sfu"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rakyll/statik/fs"
	"github.com/sourcegraph/jsonrpc2"
	websocketjsonrpc2 "github.com/sourcegraph/jsonrpc2/websocket"
	"github.com/spf13/viper"
	"go.opencensus.io/trace"
	"go.uber.org/fx"
	"net"
	"net/http"
	_ "net/http/pprof"
	"net/url"
	"nkonev.name/video/handlers"
	"os"
	"strings"
	"sync"
	. "nkonev.name/video/logger"

)

const EXTERNAL_TRACE_ID_HEADER = "trace-id"

type staticMiddleware echo.MiddlewareFunc


var (
	conf        = sfu.Config{}
	file        string
	cert        string
	key         string
	addr        string
	metricsAddr string
)

const (
	portRangeLimit = 100
)

func showHelp() {
	fmt.Printf("Usage:%s {params}\n", os.Args[0])
	fmt.Println("      -c {config file}")
	fmt.Println("      -cert {cert file}")
	fmt.Println("      -key {key file}")
	fmt.Println("      -a {listen addr}")
	fmt.Println("      -h (show help info)")
}

func load() bool {
	_, err := os.Stat(file)
	if err != nil {
		return false
	}

	viper.SetConfigFile(file)
	viper.SetConfigType("toml")

	err = viper.ReadInConfig()
	if err != nil {
		fmt.Printf("config file %s read failed. %v\n", file, err)
		return false
	}
	err = viper.GetViper().Unmarshal(&conf)
	if err != nil {
		fmt.Printf("sfu config file %s loaded failed. %v\n", file, err)
		return false
	}

	if len(conf.WebRTC.ICEPortRange) > 2 {
		fmt.Printf("config file %s loaded failed. range port must be [min,max]\n", file)
		return false
	}

	if len(conf.WebRTC.ICEPortRange) != 0 && conf.WebRTC.ICEPortRange[1]-conf.WebRTC.ICEPortRange[0] < portRangeLimit {
		fmt.Printf("config file %s loaded failed. range port must be [min, max] and max - min >= %d\n", file, portRangeLimit)
		return false
	}

	fmt.Printf("config %s load ok!\n", file)
	return true
}

func parse() bool {
	flag.StringVar(&file, "c", "config.yml", "config file")
	flag.StringVar(&cert, "cert", "", "cert file")
	flag.StringVar(&key, "key", "", "key file")
	flag.StringVar(&addr, "a", ":7000", "address to use")
	flag.StringVar(&metricsAddr, "m", ":8100", "merics to use")
	help := flag.Bool("h", false, "help info")
	flag.Parse()
	if !load() {
		return false
	}

	if *help {
		return false
	}
	return true
}

type UsersResponse struct {
	UsersCount int64 `json:"usersCount"`
}

type ConfigResponse struct {
	Urls []string `json:"urls"`
}

func main() {
	if !parse() {
		showHelp()
		os.Exit(-1)
	}

	fixByFile := []string{"asm_amd64.s", "proc.go", "icegatherer.go", "jsonrpc2"}
	fixByFunc := []string{"Handle"}
	log.Init(conf.Log.Level, fixByFile, fixByFunc)

	log.Infof("--- Starting SFU Node ---")

	app := fx.New(
		fx.Logger(Logger),
		fx.Provide(
			configureEcho,
			configureStaticMiddleware,
		),
		fx.Invoke(
			initJaeger,
			runEcho,
		),
	)
	app.Run()

	Logger.Infof("Exit program")

	////////////////////////////////////////////////////

	s := sfu.NewSFU(conf)
	dc := s.NewDatachannel(sfu.APIChannelLabel)
	dc.Use(datachannel.SubscriberAPI)

	upgrader := websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool {
			return true
		},
		ReadBufferSize:  1024,
		WriteBufferSize: 1024,
	}

	client := NewRestClient()

	sessionUserPeer := &sync.Map{}


	go startMetrics(metricsAddr)

	var err error
	if key != "" && cert != "" {
		log.Infof("Listening at https://[%s]", addr)
		err = http.ListenAndServeTLS(addr, cert, key, nil)
	} else {
		log.Infof("Listening at http://[%s]", addr)
		err = http.ListenAndServe(addr, nil)
	}
	if err != nil {
		panic(err)
	}
}

func addPeerToMap(sessionUserPeer *sync.Map, chatId string, userId string, peer *server.JSONSignal) {
	userPeerInterface, _ := sessionUserPeer.LoadOrStore(chatId, &sync.Map{})
	userPeer := userPeerInterface.(*sync.Map)
	userPeer.Store(userId, peer)
}

func removePeerFromMap(sessionUserPeer *sync.Map, chatId string, userId string) {
	userPeerInterface, ok := sessionUserPeer.Load(chatId)
	if !ok {
		log.Infof("Cannot remove chatId=%v from sessionUserPeer", chatId)
		return
	}
	userPeer := userPeerInterface.(*sync.Map)
	userPeer.Delete(userId)

	userPeerLength := countMapLen(userPeer)

	if userPeerLength == 0 {
		log.Infof("For chatId=%v there is no peers, removing user %v", chatId, userId)
		sessionUserPeer.Delete(chatId)
	}
}

func countMapLen(m *sync.Map) int64 {
	var length int64 = 0
	m.Range(func(_, _ interface{}) bool {
		length++
		return true
	})
	return length
}

func checkAccess(client *http.Client, userIdString string, chatIdString string) bool {
	url0 := viper.GetString("chat.url.base")
	url1 := viper.GetString("chat.url.access")

	response, err := client.Get(url0 + url1 + "?userId=" + userIdString + "&chatId=" + chatIdString)
	if err != nil {
		log.Errorf("Transport error during checking access %v", err)
		return false
	}
	if response.StatusCode == http.StatusOK {
		return true
	} else if response.StatusCode == http.StatusUnauthorized {
		return false
	} else {
		log.Errorf("Unexpected status on checkAccess %v", response.StatusCode)
		return false
	}
}



func configureOpencensusMiddleware() echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(ctx echo.Context) (err error) {
			handler := &ochttp.Handler{
				Handler: http.HandlerFunc(
					func(w http.ResponseWriter, r *http.Request) {
						ctx.SetRequest(r)
						ctx.SetResponse(echo.NewResponse(w, ctx.Echo()))
						existsSpan := trace.FromContext(ctx.Request().Context())
						if existsSpan != nil {
							w.Header().Set(EXTERNAL_TRACE_ID_HEADER, existsSpan.SpanContext().TraceID.String())
						}
						err = next(ctx)
					},
				),
				Propagation: &uberCompat.HTTPFormat{},
			}
			handler.ServeHTTP(ctx.Response(), ctx.Request())
			return
		}
	}
}

func createCustomHTTPErrorHandler(e *echo.Echo) func(err error, c echo.Context) {
	originalHandler := e.DefaultHTTPErrorHandler
	return func(err error, c echo.Context) {
		GetLogEntry(c.Request()).Errorf("Unhandled error: %v", err)
		originalHandler(err, c)
	}
}

func configureEcho(
	staticMiddleware staticMiddleware,
	authMiddleware handlers.AuthMiddleware,
	lc fx.Lifecycle,
	db db.DB,
	m *minio.Client,
) *echo.Echo {

	bodyLimit := viper.GetString("server.body.limit")

	e := echo.New()
	e.Logger.SetOutput(Logger.Writer())

	e.HTTPErrorHandler = createCustomHTTPErrorHandler(e)

	e.Pre(echo.MiddlewareFunc(staticMiddleware))
	e.Use(configureOpencensusMiddleware())
	e.Use(echo.MiddlewareFunc(authMiddleware))
	accessLoggerConfig := middleware.LoggerConfig{
		Output: Logger.Writer(),
		Format: `"remote_ip":"${remote_ip}",` +
			`"method":"${method}","uri":"${uri}",` +
			`"status":${status},` +
			`,"bytes_in":${bytes_in},"bytes_out":${bytes_out},"traceId":"${header:X-B3-Traceid}"` + "\n",
	}
	e.Use(middleware.LoggerWithConfig(accessLoggerConfig))
	e.Use(middleware.Secure())
	e.Use(middleware.BodyLimit(bodyLimit))

	ch := handlers.NewFileHandler(db, m)
	e.POST("/storage/avatar", ch.PutAvatar)
	e.GET(fmt.Sprintf("%v/:filename", handlers.UrlStorageGetAvatar), ch.Download)

	lc.Append(fx.Hook{
		OnStop: func(ctx context.Context) error {
			// do some work on application stop (like closing connections and files)
			Logger.Infof("Stopping http server")
			return e.Shutdown(ctx)
		},
	})

	return e
}

func configureStaticMiddleware() staticMiddleware {
	statikFS, err := fs.NewWithNamespace("assets")
	if err != nil {
		Logger.Fatal(err)
	}

	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			reqUrl := c.Request().RequestURI
			if reqUrl == "/" || reqUrl == "/index.html" || reqUrl == "/favicon.ico" || strings.HasPrefix(reqUrl, "/build") || strings.HasPrefix(reqUrl, "/assets") || reqUrl == "/git.json" {
				http.FileServer(statikFS).
					ServeHTTP(c.Response().Writer, c.Request())
				return nil
			} else {
				return next(c)
			}
		}
	}
}

func initJaeger(lc fx.Lifecycle) error {
	exporter, err := jaeger.NewExporter(jaeger.Options{
		AgentEndpoint: viper.GetString("jaeger.endpoint"),
		Process: jaeger.Process{
			ServiceName: "chat",
		},
	})
	if err != nil {
		return err
	}
	trace.RegisterExporter(exporter)
	trace.ApplyConfig(trace.Config{
		DefaultSampler: trace.AlwaysSample(),
	})
	lc.Append(fx.Hook{
		OnStop: func(ctx context.Context) error {
			Logger.Infof("Stopping tracer")
			exporter.Flush()
			trace.UnregisterExporter(exporter)
			return nil
		},
	})
	return nil
}

// rely on viper import and it's configured by
func runEcho(e *echo.Echo) {
	address := viper.GetString("server.address")

	Logger.Info("Starting server...")
	// Start server in another goroutine
	go func() {
		if err := e.Start(address); err != nil {
			Logger.Infof("server shut down: %v", err)
		}
	}()
	Logger.Info("Server started. Waiting for interrupt signal 2 (Ctrl+C)")
}
