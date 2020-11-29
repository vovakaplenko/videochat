package handlers

import (
	"github.com/centrifugal/centrifuge"
	"github.com/labstack/echo/v4"
	"io/ioutil"
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
	userId := c.QueryParam("user")
	participantChannel := h.centrifuge.PersonalChannel(userId)

	body, err := ioutil.ReadAll(c.Request().Body)
	if err != nil {
		return err
	}
	_, err = h.centrifuge.Publish(participantChannel, body)
	return err
}