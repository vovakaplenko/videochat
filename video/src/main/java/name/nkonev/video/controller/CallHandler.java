package name.nkonev.video.controller;

import java.io.IOException;

import name.nkonev.video.dto.in.JoinRoomDto;
import name.nkonev.video.dto.in.LeaveRoomDto;
import name.nkonev.video.dto.in.OnIceCandidateDto;
import name.nkonev.video.dto.in.ReceiveVideoFromDto;
import name.nkonev.video.service.Room;
import name.nkonev.video.service.RoomManager;
import org.kurento.client.IceCandidate;
import name.nkonev.video.service.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallHandler  {

  private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

  @Autowired
  private RoomManager roomManager;

  @PostMapping("/joinRoom")
  public void joinRoom(@RequestBody JoinRoomDto joinRoomDto) throws IOException {
    joinRoom(joinRoomDto.getRoomId(), joinRoomDto.getUserSessionId());
  }

  @PostMapping("/receiveVideoFrom")
  public void receiveVideoFrom(@RequestBody ReceiveVideoFromDto jsonMessage) throws IOException {
    String userSessionId = jsonMessage.getUserSessionId();
    Long roomId = jsonMessage.getRoomId();
    final Room room = roomManager.getRoom(roomId);
    final UserSession user = room.getUserSession(userSessionId);
    if (user == null) {
      log.warn("UserSession userSessionId={} not found in room {}", userSessionId, roomId);
      return;
    }

    final String senderSessionId = jsonMessage.getSenderSessionId();
    final UserSession sender = room.getUserSession(senderSessionId);
    if (sender == null) {
      log.warn("UserSession userSessionId={} not found in room {}", userSessionId, roomId);
      return;
    }

    final String sdpOffer = jsonMessage.getSdpOffer();
    user.receiveVideoFrom(sender, sdpOffer);
  }

  @PostMapping("/leaveRoom")
  public void leaveRoom(@RequestBody LeaveRoomDto leaveRoomDto) throws IOException {
    leaveRoom(leaveRoomDto.getRoomId(), leaveRoomDto.getUserSessionId());
  }

  @PostMapping("/onIceCandidate")
  public void onIceCandidate(@RequestBody OnIceCandidateDto jsonMessage) {
    String userSessionId = jsonMessage.getUserSessionId();
    Long roomId = jsonMessage.getRoomId();

    final Room room = roomManager.getRoom(roomId);
    final UserSession user = room.getUserSession(userSessionId);
    if (user == null) {
      log.warn("UserSession userSessionId={} not found in room {}", userSessionId, roomId);
      return;
    }

    OnIceCandidateDto.InternalIceCandidateDto candidate = jsonMessage.getCandidate();

    IceCandidate cand = new IceCandidate(
            candidate.getCandidate(),
            candidate.getSdpMid(),
            candidate.getSdpMLineIndex()
    );
    user.addCandidate(cand, jsonMessage.getFromUserSessionId());

  }

  private void joinRoom(Long roomId, String userSessionId) throws IOException {
    log.info("PARTICIPANT {}: trying to join room {}", userSessionId, roomId);

    Room room = roomManager.getRoom(roomId);
    final UserSession user = room.join(userSessionId);
  }

  private void leaveRoom(Long roomId, String userSessionId) throws IOException {
    final Room room = roomManager.getRoom(roomId);
    final UserSession userSession = room.getUserSession(userSessionId);
    if (userSession == null) {
      log.info("UserSession userSessionId={} not found in room {}", userSessionId, roomId);
    } else {
      room.leave(userSession);
    }
    if (room.getParticipants().isEmpty()) {
      roomManager.removeRoom(room);
    }
  }
}
