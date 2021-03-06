package config

import (
	"github.com/nkonev/ion-sfu/pkg/sfu"
	"time"
)

type RestClientConfig struct {
	MaxIdleConns int `mapstructure:"maxIdleConns"`
	IdleConnTimeout time.Duration `mapstructure:"idleConnTimeout"`
	DisableCompression bool `mapstructure:"disableCompression"`
}

type FrontendConfig struct {
	ICEServers []sfu.ICEServerConfig `mapstructure:"iceserver"`
}

type ChatConfig struct {
	ChatUrlConfig ChatUrlConfig `mapstructure:"url"`
}

type ChatUrlConfig struct {
	Base string `mapstructure:"base"`
	Access string `mapstructure:"access"`
	Notify string `mapstructure:"notify"`
}


type ExtendedConfig struct {
	sfu.Config
	FrontendConfig FrontendConfig `mapstructure:"frontend"`
	RestClientConfig RestClientConfig `mapstructure:"http"`
	ChatConfig ChatConfig `mapstructure:"chat"`
}

