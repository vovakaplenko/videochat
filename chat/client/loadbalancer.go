package client

import (
	"github.com/gomodule/redigo/redis"
	"nkonev.name/chat/listener"
	"nkonev.name/chat/logger"
)

type StickyLoadBalancer struct {
	Pool listener.VideoRedisPool
}

func (lb *StickyLoadBalancer) SelectInstanceUrl(chatId int64) (string, error) {
	c := lb.Pool.Get()
	defer c.Close()
	reply, err := redis.Values(c.Do("KEYS", "video*"))
	if err != nil {
		return "", err
	}
	logger.Logger.Info(reply)
	return
}