package main

import (
	"crypto/tls"
	"flag"
	"fmt"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"github.com/nkonev/ion-sfu/pkg/middlewares/datachannel"
	"github.com/nkonev/ion-sfu/pkg/sfu"
	log "github.com/pion/ion-log"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/spf13/viper"
	"net"
	"net/http"
	_ "net/http/pprof"
	"nkonev.name/video/config"
	"nkonev.name/video/handlers"
	"os"
)


var (
	conf        = config.ExtendedConfig{}
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
	viper.SetConfigType("yml")

	err = viper.ReadInConfig()
	if err != nil {
		fmt.Printf("config file %s read failed. %v\n", file, err)
		return false
	}
	err = viper.GetViper().Unmarshal(&conf)
	if err != nil {
		fmt.Printf("sfu extended config file %s loaded failed. %v\n", file, err)
		return false
	}
	err = viper.GetViper().Unmarshal(&conf.Config)
	if err != nil {
		fmt.Printf("sfu core config file %s loaded failed. %v\n", file, err)
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

func startMetrics(addr string) {
	// start metrics server
	m := http.NewServeMux()
	m.Handle("/metrics", promhttp.Handler())
	srv := &http.Server{
		Handler: m,
	}

	metricsLis, err := net.Listen("tcp", addr)
	if err != nil {
		log.Panicf("cannot bind to metrics endpoint %s. err: %s", addr, err)
	}
	log.Infof("Metrics Listening at %s", addr)

	err = srv.Serve(metricsLis)
	if err != nil {
		log.Errorf("debug server stopped. got err: %s", err)
	}
}

func NewRestClient() *http.Client {
	tr := &http.Transport{
		MaxIdleConns:       conf.RestClientConfig.MaxIdleConns,
		IdleConnTimeout:    conf.RestClientConfig.IdleConnTimeout,
		DisableCompression: conf.RestClientConfig.DisableCompression,
	}
	tr.TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
	client := &http.Client{Transport: tr}
	return client
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

	s := sfu.NewSFU(conf.Config)
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

	handler := handlers.NewHandler(client, &upgrader, s, &conf)
	r := mux.NewRouter()
	// SFU websocket endpoint
	r.Handle("/video/ws", http.HandlerFunc(handler.SfuHandler)).Methods("GET")
	// GET /api/video/users?chatId=${this.chatId} - responds users count
	r.Handle("/video/users", http.HandlerFunc(handler.Users)).Methods("GET")
	// PUT /api/video/notify?chatId=${this.chatId}` -> "/internal/video/notify"
	r.Handle("/video/notify", http.HandlerFunc(handler.NotifyChatParticipants)).Methods("PUT")
	// GET `/api/video/config`
	r.Handle("/video/config", http.HandlerFunc(handler.Config)).Methods("GET")
	r.PathPrefix("/").Methods("GET").HandlerFunc(handler.Static())

	go startMetrics(metricsAddr)

	var err error
	if key != "" && cert != "" {
		log.Infof("Listening at https://[%s]", addr)
		err = http.ListenAndServeTLS(addr, cert, key, r)
	} else {
		log.Infof("Listening at http://[%s]", addr)
		err = http.ListenAndServe(addr, r)
	}
	if err != nil {
		panic(err)
	}
}

