
package name.nkonev.video.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import name.nkonev.video.dto.out.ExistsParticipantsDto;
import name.nkonev.video.dto.out.NewParticipantArrivedDto;
import name.nkonev.video.dto.out.ParticipantLeftDto;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Room implements Closeable {
  private final Logger log = LoggerFactory.getLogger(Room.class);

  private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();
  private final MediaPipeline pipeline;
  private final ChatRequestService chatRequestService;
  private final Long roomId;

  public Long getName() {
    return roomId;
  }

  public Room(Long roomId, MediaPipeline pipeline, ChatRequestService chatRequestService) {
    this.roomId = roomId;
    this.pipeline = pipeline;
    this.chatRequestService = chatRequestService;
    log.info("ROOM {} has been created", roomId);
  }

  @PreDestroy
  private void shutdown() {
    this.close();
  }

  public UserSession join(String userSessionId) {
    log.info("ROOM {}: adding participant {}", this.roomId, userSessionId);
    final UserSession participant = new UserSession(userSessionId, roomId, this.pipeline, this.chatRequestService);
    sendParticipantNamesTo(participant);
    joinRoom(participant);
    participants.put(participant.getUserSessionId(), participant);
    return participant;
  }

  public void leave(UserSession user) {
    log.debug("PARTICIPANT {}: Leaving room {}", user.getUserSessionId(), roomId);
    this.removeParticipant(user.getUserSessionId());
    user.close();
  }

  private void joinRoom(UserSession newParticipant) {
    log.debug("ROOM {}: notifying other participants of new participant {}", roomId, newParticipant.getUserSessionId());
    for (final UserSession participant : participants.values()) {
      try {
        participant.sendMessage(new NewParticipantArrivedDto(newParticipant.getUserSessionId()));
      } catch (final RuntimeException e) {
        log.debug("ROOM {}: participant {} could not be notified", roomId, participant.getUserSessionId(), e);
      }
    }
  }

  private void removeParticipant(String name) {
    participants.remove(name);

    log.debug("ROOM {}: notifying all users that {} is leaving the room", roomId, name);

    final List<String> unnotifiedParticipants = new ArrayList<>();
    for (final UserSession participant : participants.values()) {
      try {
        participant.cancelVideoFrom(name);
        participant.sendMessage(new ParticipantLeftDto(name));
      } catch (final RuntimeException e) {
        unnotifiedParticipants.add(participant.getUserSessionId());
      }
    }

    if (!unnotifiedParticipants.isEmpty()) {
      log.debug("ROOM {}: The users {} could not be notified that {} left the room", roomId,
          unnotifiedParticipants, name);
    }

  }

  private void sendParticipantNamesTo(UserSession user)  {
    final List<String> participantsArray = participants.values().stream()
            .filter(userSession -> user.getUserSessionId() != null && !user.getUserSessionId().equals(userSession.getUserSessionId()))
            .map(UserSession::getUserSessionId)
            .collect(Collectors.toList());
    log.debug("PARTICIPANT {}: sending a list of {} participants", user.getUserSessionId(), participantsArray.size());
    user.sendMessage(new ExistsParticipantsDto(participantsArray));
  }

  public Collection<UserSession> getParticipants() {
    return participants.values();
  }

  public UserSession getParticipant(String name) {
    return participants.get(name);
  }

  @Override
  public void close() {
    for (final UserSession user : participants.values()) {
      try {
        user.close();
      } catch (RuntimeException e) {
        log.debug("ROOM {}: Could not invoke close on participant {}", roomId, user.getUserSessionId(), e);
      }
    }

    participants.clear();

    pipeline.release(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        log.trace("ROOM {}: Released Pipeline", roomId);
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.warn("PARTICIPANT {}: Could not release Pipeline", roomId);
      }
    });

    log.debug("Room {} closed", roomId);
  }

  public UserSession getUserSession(String userSessionId) {
    return participants.get(userSessionId);
  }
}
