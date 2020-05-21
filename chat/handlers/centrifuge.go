package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/araddon/dateparse"
	"github.com/centrifugal/centrifuge"
	"github.com/centrifugal/protocol"
	"go.uber.org/fx"
	. "nkonev.name/chat/logger"
	"time"
)

func handleLog(e centrifuge.LogEntry) {
	Logger.Printf("%s: %v", e.Message, e.Fields)
}

func getChanPresenceStats(engine centrifuge.Engine, client *centrifuge.Client, e interface{}) *centrifuge.PresenceStats {
	var channel string
	switch v := e.(type) {
	case centrifuge.SubscribeEvent:
		channel = v.Channel
		break
	case centrifuge.UnsubscribeEvent:
		channel = v.Channel
		break
	default:
		Logger.Errorf("Unknown type of event")
		return nil
	}
	stats, err := engine.PresenceStats(channel)
	if err != nil {
		Logger.Errorf("Error during get stats %v", err)
	}
	Logger.Printf("client id=%v, userId=%v subscribes on channel %s, channelStats.NumUsers %v", client.ID(), client.UserID(), channel, stats.NumUsers)
	return &stats
}

func ConfigureCentrifuge(lc fx.Lifecycle) *centrifuge.Node {
	// We use default config here as starting point. Default config contains
	// reasonable values for available options.
	cfg := centrifuge.DefaultConfig
	// In this example we want client to do all possible actions with server
	// without any authentication and authorization. Insecure flag DISABLES
	// many security related checks in library. This is only to make example
	// short. In real app you most probably want authenticate and authorize
	// access to server. See godoc and examples in repo for more details.
	cfg.ClientInsecure = false
	// By default clients can not publish messages into channels. Setting this
	// option to true we allow them to publish.
	cfg.Publish = true

	// Centrifuge library exposes logs with different log level. In your app
	// you can set special function to handle these log entries in a way you want.
	cfg.LogLevel = centrifuge.LogLevelDebug
	cfg.LogHandler = handleLog

	// Node is the core object in Centrifuge library responsible for many useful
	// things. Here we initialize new Node instance and pass config to it.
	node, _ := centrifuge.New(cfg)

	engine, _ := centrifuge.NewMemoryEngine(node, centrifuge.MemoryEngineConfig{})
	node.SetEngine(engine)

	// ClientConnected node event handler is a point where you generally create a
	// binding between Centrifuge and your app business logic. Callback function you
	// pass here will be called every time new connection established with server.
	// Inside this callback function you can set various event handlers for connection.
	node.On().ClientConnected(func(ctx context.Context, client *centrifuge.Client) {
		// Set Subscribe Handler to react on every channel subscribtion attempt
		// initiated by client. Here you can theoretically return an error or
		// disconnect client from server if needed. But now we just accept
		// all subscriptions.
		var credso, ok = centrifuge.GetCredentials(ctx)
		Logger.Infof("Connected websocket centrifuge client hasCredentials %v, credentials %v", ok, credso)

		client.On().Subscribe(func(e centrifuge.SubscribeEvent) centrifuge.SubscribeReply {
			expiresInString := fmt.Sprintf("%v000", credso.ExpireAt) // to milliseconds for put into dateparse.ParseLocal
			t, err0 := dateparse.ParseLocal(expiresInString)
			if err0 != nil {
				Logger.Errorf("Error during ParseLocal %v", err0)
			}

			presenceDuration := t.Sub(time.Now())
			Logger.Infof("Calculated session duration %v for credentials %v", presenceDuration, credso)

			clientInfo := &protocol.ClientInfo{
				User:   client.ID(),
				Client: client.UserID(),
			}
			err := engine.AddPresence(e.Channel, client.UserID(), clientInfo, presenceDuration)
			if err != nil {
				Logger.Errorf("Error during AddPresence %v", err)
			}

			if e.Channel == "aux" {
				stats := getChanPresenceStats(engine, client, e)

				type AuxChannelRequest struct {
					MessageType string `json:"type"`
				}

				if stats.NumUsers == 1 {
					data, _ := json.Marshal(AuxChannelRequest{"created"})
					Logger.Infof("Publishing created to channel %v", e.Channel)
					//err := node.Publish(e.Channel, data)
					err := client.Send(data)
					if err != nil {
						Logger.Errorf("Error during publishing created %v", err)
					}
				} else if stats.NumUsers > 1 {
					data, _ := json.Marshal(AuxChannelRequest{"joined"})
					Logger.Infof("Publishing joined to channel %v", e.Channel)
					// send to existing subscribers
					_, err := node.Publish(e.Channel, data)
					if err != nil {
						Logger.Errorf("Error during publishing joined %v", err)
					}
					// send to just subscribing client
					err2 := client.Send(data)
					if err2 != nil {
						Logger.Errorf("Error during publishing joined %v", err2)
					}

				}
			}

			return centrifuge.SubscribeReply{}
		})

		client.On().Unsubscribe(func(e centrifuge.UnsubscribeEvent) centrifuge.UnsubscribeReply {
			err := engine.RemovePresence(e.Channel, client.UserID())
			if err != nil {
				Logger.Errorf("Error during RemovePresence %v", err)
			}
			getChanPresenceStats(engine, client, e)

			return centrifuge.UnsubscribeReply{}
		})

		// Set Publish Handler to react on every channel Publication sent by client.
		// Inside this method you can validate client permissions to publish into
		// channel. But in our simple chat app we allow everyone to publish into
		// any channel.
		client.On().Publish(func(e centrifuge.PublishEvent) centrifuge.PublishReply {
			Logger.Printf("client publishes into channel %s: %s", e.Channel, string(e.Data))
			return centrifuge.PublishReply{}
		})

		// Set Disconnect Handler to react on client disconnect events.
		client.On().Disconnect(func(e centrifuge.DisconnectEvent) centrifuge.DisconnectReply {
			Logger.Printf("client disconnected")
			return centrifuge.DisconnectReply{}
		})

		// In our example transport will always be Websocket but it can also be SockJS.
		transportName := client.Transport().Name()
		// In our example clients connect with JSON protocol but it can also be Protobuf.
		transportEncoding := client.Transport().Encoding()

		Logger.Printf("client connected via %s (%s)", transportName, transportEncoding)
	})

	lc.Append(fx.Hook{
		OnStop: func(ctx context.Context) error {
			// do some work on application stop (like closing connections and files)
			Logger.Infof("Stopping centrifuge")
			return node.Shutdown(ctx)
		},
	})

	return node
}