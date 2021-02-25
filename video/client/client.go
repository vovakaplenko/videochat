package client

import (
	"crypto/tls"
	"github.com/spf13/viper"
	"net/http"
)

func NewRestClient() *http.Client {
	tr := &http.Transport{
		MaxIdleConns:       viper.GetInt("http.idle.conns.max"),
		IdleConnTimeout:    viper.GetDuration("http.idle.connTimeout"),
		DisableCompression: viper.GetBool("http.disableCompression"),
	}
	tr.TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
	client := &http.Client{Transport: tr}
	return client
}
