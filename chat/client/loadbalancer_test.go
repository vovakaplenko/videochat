package client

import (
	"github.com/stretchr/testify/assert"
	"nkonev.name/chat/listener"
	"nkonev.name/chat/utils"
	"os"
	"testing"
)

func TestMain(m *testing.M) {
	setup()
	retCode := m.Run()
	shutdown()
	os.Exit(retCode)
}

func shutdown() {
}

func setup() {
}

func TestBalance(t *testing.T) {
	configFile := utils.InitFlags("../config-dev/config.yml")
	utils.InitViper(configFile, "")

	rc, err:= listener.RedisVideoConnection(nil)
	assert.Nil(t, err)

	conn := rc.Get()
	defer conn.Close()
	_, err = conn.Do("SET", "video::http://localhost:8085", "true", "EX", "60")
	assert.Nil(t, err)
	_, err = conn.Do("SET", "video::http://localhost:8086", "true", "EX", "60")
	assert.Nil(t, err)

	lb := NewStickyLoadBalancer(rc)

	first, err := lb.SelectInstanceUrl(1)
	assert.Nil(t, err)
	assert.Equal(t, "http://localhost:8086", first)

	second, err := lb.SelectInstanceUrl(2)
	assert.Nil(t, err)
	assert.Equal(t, "http://localhost:8085", second)
}
