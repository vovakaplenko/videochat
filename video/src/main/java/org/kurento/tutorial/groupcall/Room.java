
package org.kurento.tutorial.groupcall;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


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

  public UserSession join(String userSessionId) throws IOException {
    log.info("ROOM {}: adding participant {}", this.roomId, userSessionId);
    final UserSession participant = new UserSession(userSessionId, roomId, this.pipeline, this.chatRequestService);
    joinRoom(participant);
    participants.put(participant.getUserSessionId(), participant);
    sendParticipantNames(participant);
    return participant;
  }

  public void leave(UserSession user) throws IOException {
    log.debug("PARTICIPANT {}: Leaving room {}", user.getUserSessionId(), roomId);
    this.removeParticipant(user.getUserSessionId());
    user.close();
  }

  private void joinRoom(UserSession newParticipant) throws IOException {
    final JsonObject newParticipantMsg = new JsonObject();
    newParticipantMsg.addProperty("id", "newParticipantArrived");
    newParticipantMsg.addProperty("name", newParticipant.getUserSessionId());
    newParticipantMsg.addProperty("userSessionId", newParticipant.getUserSessionId());

    log.debug("ROOM {}: notifying other participants of new participant {}", roomId,
        newParticipant.getUserSessionId());

    for (final UserSession participant : participants.values()) {
      try {
        participant.sendMessage(newParticipantMsg);
      } catch (final IOException e) {
        log.debug("ROOM {}: participant {} could not be notified", roomId, participant.getUserSessionId(), e);
      }
    }
  }

  private void removeParticipant(String name) throws IOException {
    participants.remove(name);

    log.debug("ROOM {}: notifying all users that {} is leaving the room", roomId, name);

    final List<String> unnotifiedParticipants = new ArrayList<>();
    final JsonObject participantLeftJson = new JsonObject();
    participantLeftJson.addProperty("id", "participantLeft");
    participantLeftJson.addProperty("name", name);
    participantLeftJson.addProperty("userSessionId", name);
    for (final UserSession participant : participants.values()) {
      try {
        participant.cancelVideoFrom(name);
        participant.sendMessage(participantLeftJson);
      } catch (final IOException e) {
        unnotifiedParticipants.add(participant.getUserSessionId());
      }
    }

    if (!unnotifiedParticipants.isEmpty()) {
      log.debug("ROOM {}: The users {} could not be notified that {} left the room", roomId,
          unnotifiedParticipants, name);
    }

  }

  public void sendParticipantNames(UserSession user) throws IOException {

    final JsonArray participantsArray = new JsonArray();
    for (final UserSession participant : this.getParticipants()) {
      if (!participant.equals(user)) {
        final JsonElement participantName = new JsonPrimitive(participant.getUserSessionId());
        participantsArray.add(participantName);
      }
    }

    final JsonObject existingParticipantsMsg = new JsonObject();
    existingParticipantsMsg.addProperty("id", "existingParticipants");
    existingParticipantsMsg.add("data", participantsArray);
    log.debug("PARTICIPANT {}: sending a list of {} participants", user.getUserSessionId(),
        participantsArray.size());
    user.sendMessage(existingParticipantsMsg);
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
      } catch (IOException e) {
        log.debug("ROOM {}: Could not invoke close on participant {}", roomId, user.getUserSessionId(),
            e);
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
