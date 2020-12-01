package name.nkonev.video.controller;

import java.io.IOException;

import name.nkonev.video.dto.in.*;
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
  public static final String INVOKE = "/invoke";

  @Autowired
  private RoomManager roomManager;

  @PostMapping(INVOKE)
  public void invoke(@RequestBody AuthData invokeDto, @RequestParam Long roomId, @RequestParam String userSessionId ) {
    // TODO refine
    invokeDto.setRoomId(roomId);
    invokeDto.setUserSessionId(userSessionId);
    if (invokeDto instanceof JoinRoomDto) {
      joinRoom((JoinRoomDto) invokeDto);
    } if (invokeDto instanceof NotifyAboutJoinRoomDto) {
      notifyAboutJoin((NotifyAboutJoinRoomDto)invokeDto);
    } if (invokeDto instanceof ReceiveVideoFromDto) {
      receiveVideoFrom((ReceiveVideoFromDto) invokeDto);
    } if (invokeDto instanceof LeaveRoomDto) {
      leaveRoom((LeaveRoomDto) invokeDto);
    } if (invokeDto instanceof OnIceCandidateDto) {
      onIceCandidate((OnIceCandidateDto) invokeDto);
    }
  }

  private void joinRoom(JoinRoomDto joinRoomDto) {
    joinRoom(joinRoomDto.getRoomId(), joinRoomDto.getUserSessionId());
  }

  private void receiveVideoFrom(ReceiveVideoFromDto jsonMessage) {
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

    final String sdpOffer = jsonMessage.getSdpOffer().toString();
    user.receiveVideoFrom(sender, sdpOffer);
  }

  private void leaveRoom(LeaveRoomDto leaveRoomDto)  {
    leaveRoom(leaveRoomDto.getRoomId(), leaveRoomDto.getUserSessionId());
  }

  private void onIceCandidate(OnIceCandidateDto jsonMessage) {
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

  private void joinRoom(Long roomId, String userSessionId) {
    log.info("PARTICIPANT {}: trying to join room {}", userSessionId, roomId);

    Room room = roomManager.getRoom(roomId);
    final UserSession user = room.join(userSessionId);
  }

  private void notifyAboutJoin(NotifyAboutJoinRoomDto invokeDto) {
    Room room = roomManager.getRoom(invokeDto.getRoomId());
    final UserSession user = room.getUserSession(invokeDto.getUserSessionId());
    room.notifyOtherParticipants(user);
  }


  private void leaveRoom(Long roomId, String userSessionId) {
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
