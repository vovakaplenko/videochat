package client

import (
	"errors"
	"github.com/gomodule/redigo/redis"
	"nkonev.name/chat/listener"
	"nkonev.name/chat/logger"
	"sort"
	"strings"
)

type StickyLoadBalancer interface {
	SelectInstanceUrl(chatId int64) (string, error)
}

type stickyLoadBalancerImpl struct {
	pool listener.VideoRedisPool
}

func NewStickyLoadBalancer(p listener.VideoRedisPool) StickyLoadBalancer {
	return stickyLoadBalancerImpl{p}
}

func (lb stickyLoadBalancerImpl) SelectInstanceUrl(chatId int64) (string, error) {
	c := lb.pool.Get()
	defer c.Close()
	reply, err := redis.Strings(c.Do("KEYS", "video*"))
	if err != nil {
		return "", err
	}
	logger.Logger.Debug(reply)
	sort.Strings(reply)
	var addresses = []string{}
	for _, withPrefixAddress := range reply {
		adr := strings.ReplaceAll(withPrefixAddress, "video::", "")
		addresses = append(addresses, adr)
	}
	length := int64(len(addresses))
	if length == 0 {
		return "", errors.New("no video servers registered in redis")
	}

	idx := int(chatId % length)
	return addresses[idx], nil
}
