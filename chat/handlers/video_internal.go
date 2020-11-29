package handlers

import (
	"github.com/centrifugal/centrifuge"
	"github.com/labstack/echo/v4"
	"io/ioutil"
	. "nkonev.name/chat/logger"
)

type VideoHandler struct {
	centrifuge *centrifuge.Node
}

func NewVideoHandler(centrifuge *centrifuge.Node) VideoHandler {
	return VideoHandler{
		centrifuge,
	}
}

func (h VideoHandler) SendToUser(c echo.Context) error {
	userId := c.QueryParam("toUser")
	participantChannel := h.centrifuge.PersonalChannel(userId)

	body, err := ioutil.ReadAll(c.Request().Body)
	if err != nil {
		return err
	}
	Logger.Infof("video -> browser userId=%v, body=%v", userId, string(body))
	_, err = h.centrifuge.Publish(participantChannel, body)
	return err
}