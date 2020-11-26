package listener

import (
	"context"
	"github.com/gomodule/redigo/redis"
	"github.com/spf13/viper"
	"go.uber.org/fx"
	. "nkonev.name/chat/logger"
	"time"
)

type AaaRedisPool struct {
	*redis.Pool
}

type VideoRedisPool struct {
	*redis.Pool
}

func RedisAaaConnection(lc fx.Lifecycle) (AaaRedisPool, error) {
	Logger.Infof("Starting redis aaa connection")

	address := viper.GetString("aaa.redis.address")
	password := viper.GetString("aaa.redis.password")

	readDuration := viper.GetDuration("aaa.redis.readTimeout")
	writeDuration := viper.GetDuration("aaa.redis.writeTimeout")
	connectTimeout := viper.GetDuration("aaa.redis.connectTimeout")
	idleTimeout := viper.GetDuration("aaa.redis.idleTimeout")
	dbase := viper.GetInt("aaa.redis.db")
	maxIdle := viper.GetInt("aaa.redis.maxIdle")
	maxActive := viper.GetInt("aaa.redis.maxActive")

	pool := &redis.Pool{
		MaxIdle:     maxIdle,
		MaxActive:   maxActive,
		Wait:        true,
		IdleTimeout: idleTimeout,
		Dial: func() (redis.Conn, error) {
			var err error

			c, err := redis.Dial("tcp", address,
				redis.DialReadTimeout(readDuration), // default 0 means infinity read
				redis.DialWriteTimeout(writeDuration),
				redis.DialConnectTimeout(connectTimeout),
				redis.DialDatabase(dbase),
				redis.DialPassword(password),
				)
			if err != nil {
				Logger.Errorf("error dialing to aaa Redis %v", err.Error())
				return nil, err
			}
			return c, nil
		},
		TestOnBorrow: func(c redis.Conn, t time.Time) error {
			_, err := c.Do("PING")
			return err
		},
	}
	lc.Append(fx.Hook{
		OnStop: func(ctx context.Context) error {
			Logger.Infof("Stopping redis aaa connection")
			return pool.Close()
		},
	})
	return AaaRedisPool{pool}, nil
}

func RedisVideoConnection(lc fx.Lifecycle) (VideoRedisPool, error) {
	Logger.Infof("Starting redis video connection")

	address := viper.GetString("video.redis.address")
	password := viper.GetString("video.redis.password")

	readDuration := viper.GetDuration("video.redis.readTimeout")
	writeDuration := viper.GetDuration("video.redis.writeTimeout")
	connectTimeout := viper.GetDuration("video.redis.connectTimeout")
	idleTimeout := viper.GetDuration("video.redis.idleTimeout")
	dbase := viper.GetInt("video.redis.db")
	maxIdle := viper.GetInt("video.redis.maxIdle")
	maxActive := viper.GetInt("video.redis.maxActive")

	pool := &redis.Pool{
		MaxIdle:     maxIdle,
		MaxActive:   maxActive,
		Wait:        true,
		IdleTimeout: idleTimeout,
		Dial: func() (redis.Conn, error) {
			var err error

			c, err := redis.Dial("tcp", address,
				redis.DialReadTimeout(readDuration), // default 0 means infinity read
				redis.DialWriteTimeout(writeDuration),
				redis.DialConnectTimeout(connectTimeout),
				redis.DialDatabase(dbase),
				redis.DialPassword(password),
			)
			if err != nil {
				Logger.Errorf("error dialing to video Redis %v", err.Error())
				return nil, err
			}
			return c, nil
		},
		TestOnBorrow: func(c redis.Conn, t time.Time) error {
			_, err := c.Do("PING")
			return err
		},
	}
	lc.Append(fx.Hook{
		OnStop: func(ctx context.Context) error {
			Logger.Infof("Stopping redis video connection")
			return pool.Close()
		},
	})
	return VideoRedisPool{pool}, nil
}




// https://pkg.go.dev/github.com/gomodule/redigo/redis#PubSubConn
func ListenPubSubChannels(
	pool AaaRedisPool,
	onMessage AaaUserProfileUpdateListener,
	lc fx.Lifecycle) {

	go func() {
		lifecycleHookAppended := false

		var psc redis.PubSubConn
		for {
			conn := pool.Get()
			defer conn.Close()

			Logger.Infof("Starting redis aaa subscription")
			var channels []string = []string{"user.profile.update"}

			psc = redis.PubSubConn{Conn: conn}

			if err := psc.Subscribe(redis.Args{}.AddFlat(channels)...); err != nil {
				Logger.Errorf("Error on aaa subscription %v", err)
				sleepBetweenRetry()
				continue
			}

			if !lifecycleHookAppended {
				// Signal the receiving goroutine to exit by unsubscribing from all channels.
				lc.Append(fx.Hook{
					OnStop: func(ctx context.Context) error {
						Logger.Infof("Removing redis aaa subscription")
						return psc.Unsubscribe()
					},
				})
				lifecycleHookAppended = true
			}

			done := make(chan error, 1)

			// Start a goroutine to receive notifications from the server.
			go func() {
				for {
					switch n := psc.Receive().(type) {
					case error:
						done <- n
						return
					case redis.Message:
						if err := onMessage(n.Channel, n.Data); err != nil {
							done <- err
							return
						}
					case redis.Subscription:
						switch n.Count {
						case len(channels):
							// Notify application when all channels are subscribed.
							Logger.Infof("app subscribed to the all channels")
						case 0:
							// Return from the goroutine when all channels are unsubscribed.
							done <- nil
							return
						}
					}
				}
			}()

			// Wait for goroutine to complete.
			err := <-done
			Logger.Errorf("Error on redis aaa subscription %v", err)

			err = psc.Unsubscribe()
			Logger.Infof("Unsubscribing, error=%v", err)

			sleepBetweenRetry()
		}
	}()
}

func sleepBetweenRetry() {
	const sleepSec = 1
	Logger.Infof("Sleep %v sec", sleepSec)
	time.Sleep(sleepSec * time.Second)
}
