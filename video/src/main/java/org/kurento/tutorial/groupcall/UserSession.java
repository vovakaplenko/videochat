/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.groupcall;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.Continuation;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.3.1
 */
public class UserSession implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(UserSession.class);

  private final String userSessionId;

  private final MediaPipeline pipeline;

  private final Long roomId;
  private final WebRtcEndpoint outgoingMedia;
  private final ChatRequestService chatRequestService;
  // UserSessionId : WebRtcEndpoint
  private final ConcurrentMap<String, WebRtcEndpoint> incomingMediaMap = new ConcurrentHashMap<>();

  public UserSession(final String userSessionId, Long roomId,
      MediaPipeline pipeline, ChatRequestService chatRequestService) {

    this.pipeline = pipeline;
    this.userSessionId = userSessionId;
    this.chatRequestService = chatRequestService;
    this.roomId = roomId;
    this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();

    this.outgoingMedia.addIceCandidateFoundListener(new OutgoingWebRtcEndpointIceCandidateFoundListener(userSessionId, chatRequestService));
  }

  public WebRtcEndpoint getOutgoingWebRtcPeer() {
    return outgoingMedia;
  }

  public String getName() {
    return userSessionId;
  }

  /**
   * The room to which the user is currently attending.
   *
   * @return The room
   */
  public Long getRoomId() {
    return this.roomId;
  }

  public void receiveVideoFrom(UserSession sender, String sdpOffer) throws IOException {
    log.info("USER {}: connecting with {} in room {}", getName(), sender.getName(), getRoomId());

    log.trace("USER {}: SdpOffer for {} is {}", getName(), sender.getName(), sdpOffer);

    final String ipSdpAnswer = this.getEndpointForUser(sender).processOffer(sdpOffer);
    final JsonObject scParams = new JsonObject();
    scParams.addProperty("id", "receiveVideoAnswer");
    scParams.addProperty("name", sender.getName());
    scParams.addProperty("userSessionId", sender.getName());
    scParams.addProperty("sdpAnswer", ipSdpAnswer);

    log.trace("USER {}: SdpAnswer for {} is {}", getName(), sender.getName(), ipSdpAnswer);
    this.sendMessage(scParams);
    log.debug("gather candidates");
    this.getEndpointForUser(sender).gatherCandidates();
  }

  private WebRtcEndpoint getEndpointForUser(final UserSession sender) {
    if (sender.getName().equals(getName())) {
      log.debug("PARTICIPANT {}: configuring loopback", getName());
      return outgoingMedia;
    }

    log.debug("PARTICIPANT {}: receiving video from {}", getName(), sender.getName());

    WebRtcEndpoint incomingEndpoint = incomingMediaMap.get(sender.getName());
    if (incomingEndpoint == null) {
      log.debug("PARTICIPANT {}: creating new endpoint for {}", getName(), sender.getName());
      incomingEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

      incomingEndpoint.addIceCandidateFoundListener(new IncomingWebRtcEndpointIceCandidateFoundListener(userSessionId, sender.getName(), chatRequestService));

      incomingMediaMap.put(sender.getName(), incomingEndpoint);
    }

    log.debug("PARTICIPANT {}: obtained endpoint for {}", getName(), sender.getName());
    sender.getOutgoingWebRtcPeer().connect(incomingEndpoint);

    return incomingEndpoint;
  }

  public void cancelVideoFrom(final UserSession sender) {
    this.cancelVideoFrom(sender.getName());
  }

  public void cancelVideoFrom(final String senderName) {
    log.debug("PARTICIPANT {}: canceling video reception from {}", getName(), senderName);
    final WebRtcEndpoint incoming = incomingMediaMap.remove(senderName);

    log.debug("PARTICIPANT {}: removing endpoint for {}", getName(), senderName);
    incoming.release(new Continuation<Void>() {
      @Override
      public void onSuccess(Void result) throws Exception {
        log.trace("PARTICIPANT {}: Released successfully incoming EP for {}", getName(), senderName);
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.warn("PARTICIPANT {}: Could not release incoming EP for {}", getName(),
            senderName);
      }
    });
  }

  @Override
  public void close() throws IOException {
    log.debug("PARTICIPANT {}: Releasing resources", getName());
    for (final String remoteParticipantName : incomingMediaMap.keySet()) {
      log.trace("PARTICIPANT {}: Released incoming EP for {}", getName(), remoteParticipantName);
      final WebRtcEndpoint ep = incomingMediaMap.get(remoteParticipantName);
      ep.release(new Continuation<Void>() {

        @Override
        public void onSuccess(Void result) throws Exception {
          log.trace("PARTICIPANT {}: Released successfully incoming EP for {}", getName(), remoteParticipantName);
        }

        @Override
        public void onError(Throwable cause) throws Exception {
          log.warn("PARTICIPANT {}: Could not release incoming EP for {}", getName(),
              remoteParticipantName);
        }
      });
    }

    outgoingMedia.release(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        log.trace("PARTICIPANT {}: Released outgoing EP", getName());
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.warn("USER {}: Could not release outgoing EP", getName());
      }
    });
  }

  public void sendMessage(JsonObject message) throws IOException {
    log.debug("USER {}: Sending message {}", getName(), message);

    chatRequestService.sendToWebsocketForSession(userSessionId, message);
  }

  public void addCandidate(IceCandidate candidate, String name) {
    if (getName().compareTo(name) == 0) {
      outgoingMedia.addIceCandidate(candidate);
    } else {
      WebRtcEndpoint webRtc = incomingMediaMap.get(name);
      if (webRtc != null) {
        webRtc.addIceCandidate(candidate);
      }
    }
  }

}


class OutgoingWebRtcEndpointIceCandidateFoundListener implements EventListener<IceCandidateFoundEvent> {
  private final String userSessionId;
  private final ChatRequestService chatRequestService;

  OutgoingWebRtcEndpointIceCandidateFoundListener(String userSessionId, ChatRequestService chatRequestService) {
    this.userSessionId = userSessionId;
    this.chatRequestService = chatRequestService;
  }


  @Override
  public void onEvent(IceCandidateFoundEvent event) {
    JsonObject response = new JsonObject();
    response.addProperty("id", "iceCandidate");
    response.addProperty("name", userSessionId);
    response.addProperty("userSessionId", userSessionId);
    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));

    chatRequestService.sendToWebsocketForSession(userSessionId, response);
  }
}

class IncomingWebRtcEndpointIceCandidateFoundListener implements EventListener<IceCandidateFoundEvent> {

  private final String userSessionId; // my
  private final String otherUserSessionId; // other user
  private final ChatRequestService chatRequestService;

  IncomingWebRtcEndpointIceCandidateFoundListener(String userSessionId, String otherUserSessionId, ChatRequestService chatRequestService) {
    this.userSessionId = userSessionId;
    this.otherUserSessionId = otherUserSessionId;
    this.chatRequestService = chatRequestService;
  }

  @Override
  public void onEvent(IceCandidateFoundEvent event) {
    JsonObject response = new JsonObject();
    response.addProperty("id", "iceCandidate");
    response.addProperty("name", otherUserSessionId);
    response.addProperty("userSessionId", otherUserSessionId);
    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));

    chatRequestService.sendToWebsocketForSession(userSessionId, response);
  }

}