package handlers

import (
	"encoding/json"
	"github.com/spf13/viper"
	"net/http"
)

type Sfu struct {

}

func NewSfuHandler() Sfu {
	return Sfu{}
}

http.Handle("/video/ws", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	userId := r.Header.Get("X-Auth-UserId")
	chatId := r.URL.Query().Get("chatId")
	if !checkAccess(client, userId, chatId) {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	c, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		panic(err)
	}
	defer c.Close()

	p := server.NewJSONSignal(sfu.NewPeer(s))
	addPeerToMap(sessionUserPeer, chatId, userId, p)
	defer p.Close()
	defer removePeerFromMap(sessionUserPeer, chatId, userId)

	jc := jsonrpc2.NewConn(r.Context(), websocketjsonrpc2.NewObjectStream(c), p)
	<-jc.DisconnectNotify()
}))

// GET /api/video/users?chatId=${this.chatId} - responds users count
http.Handle("/video/users", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	userId := r.Header.Get("X-Auth-UserId")
	chatId := r.URL.Query().Get("chatId")
	if !checkAccess(client, userId, chatId) {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	chatInterface, ok := sessionUserPeer.Load(chatId)
	response := UsersResponse{}
	if ok {
		chat := chatInterface.(*sync.Map)
		response.UsersCount = countMapLen(chat)
	}
	w.Header().Set("Content-Type", "application/json")
	marshal, err := json.Marshal(response)
	if err != nil {
		log.Errorf("Error during marshalling UsersResponse to json")
		w.WriteHeader(http.StatusInternalServerError)
	} else {
		_, err := w.Write(marshal)
		if err != nil {
			log.Errorf("Error during sending json")
		}
	}
}))

// PUT /api/video/notify?chatId=${this.chatId}` -> "/internal/video/notify"
http.Handle("/video/notify", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	userId := r.Header.Get("X-Auth-UserId")
	chatId := r.URL.Query().Get("chatId")
	if !checkAccess(client, userId, chatId) {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	var usersCount int64 = 0
	chatInterface, ok := sessionUserPeer.Load(chatId)
	if ok {
		chat := chatInterface.(*sync.Map)
		usersCount = countMapLen(chat)
	}

	url0 := viper.GetString("chat.url.base")
	url1 := viper.GetString("chat.url.notify")

	fullUrl := fmt.Sprintf("%v%v?usersCount=%v&chatId=%v", url0, url1, usersCount, chatId)
	parsedUrl, err := url.Parse(fullUrl)
	if err != nil {
		log.Errorf("Failed during parse chat url: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	req := &http.Request{Method: http.MethodPut, URL: parsedUrl}

	response, err := client.Do(req)
	if err != nil {
		log.Errorf("Transport error during notifying %v", err)
		w.WriteHeader(http.StatusInternalServerError)
	} else {
		if response.StatusCode != http.StatusOK {
			log.Errorf("Http Error %v during notifying %v", response.StatusCode, err)
			w.WriteHeader(http.StatusInternalServerError)
		}
	}
}))

// GET `/api/video/config`
http.Handle("/video/config", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	urls := viper.GetStringSlice("frontend.urls")
	response := ConfigResponse{Urls: urls}
	w.Header().Set("Content-Type", "application/json")
	marshal, err := json.Marshal(response)
	if err != nil {
		log.Errorf("Error during marshalling ConfigResponse to json")
		w.WriteHeader(http.StatusInternalServerError)
	} else {
		_, err := w.Write(marshal)
		if err != nil {
			log.Errorf("Error during sending json")
		}
	}
}))
