package name.nkonev.video.service;

import name.nkonev.video.dto.out.IceCandidateDto;
import name.nkonev.video.dto.out.ReceiveVideoAnswerFromDto;
import name.nkonev.video.dto.out.Typed;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserSession implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(UserSession.class);

    private final String userSessionId;

    private final MediaPipeline roomMediaPipeline;

    private final Long roomId;
    private final WebRtcEndpoint outgoingMedia;
    private final ChatRequestService chatRequestService;
    // UserSessionId : WebRtcEndpoint
    private final ConcurrentMap<String, WebRtcEndpoint> incomingMediaMap = new ConcurrentHashMap<>();

    // TODO modifying
    // TODO subscriptionId modifying
    public UserSession(final String userSessionId, Long roomId,
                       MediaPipeline pipeline, ChatRequestService chatRequestService) {

        this.roomMediaPipeline = pipeline;
        this.userSessionId = userSessionId;
        this.chatRequestService = chatRequestService;
        this.roomId = roomId;
        this.outgoingMedia = new WebRtcEndpoint.Builder(roomMediaPipeline).build();

        this.outgoingMedia.addIceCandidateFoundListener(new OutgoingWebRtcEndpointIceCandidateFoundListener(userSessionId, chatRequestService));
    }

    public WebRtcEndpoint getOutgoingWebRtcPeer() {
        return outgoingMedia;
    }

    public String getUserSessionId() {
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

    public void receiveVideoFrom(UserSession sender, String sdpOffer) {
        log.info("USER {}: connecting with {} in room {}", getUserSessionId(), sender.getUserSessionId(), getRoomId());
        log.trace("USER {}: SdpOffer for {} is {}", getUserSessionId(), sender.getUserSessionId(), sdpOffer);
        final String ipSdpAnswer = this.getEndpointForUser(sender).processOffer(sdpOffer);
        log.trace("USER {}: SdpAnswer for {} is {}", getUserSessionId(), sender.getUserSessionId(), ipSdpAnswer);
        this.sendMessage(new ReceiveVideoAnswerFromDto(sender.getUserSessionId(), ipSdpAnswer));
        log.debug("gather candidates");
        this.getEndpointForUser(sender).gatherCandidates();
    }

    // TODO modifying
    // TODO subscriptionId modifying
    private WebRtcEndpoint getEndpointForUser(final UserSession sender) {
        if (sender.getUserSessionId().equals(getUserSessionId())) {
            log.debug("PARTICIPANT {}: configuring loopback", getUserSessionId());
            return outgoingMedia;
        }

        log.debug("PARTICIPANT {}: receiving video from {}", getUserSessionId(), sender.getUserSessionId());

        WebRtcEndpoint incomingEndpoint = incomingMediaMap.get(sender.getUserSessionId());
        if (incomingEndpoint == null) {
            log.debug("PARTICIPANT {}: creating new endpoint for {}", getUserSessionId(), sender.getUserSessionId());
            incomingEndpoint = new WebRtcEndpoint.Builder(roomMediaPipeline).build();

            incomingEndpoint.addIceCandidateFoundListener(new IncomingWebRtcEndpointIceCandidateFoundListener(userSessionId, sender.getUserSessionId(), chatRequestService));

            incomingMediaMap.put(sender.getUserSessionId(), incomingEndpoint);
        }

        log.debug("PARTICIPANT {}: obtained endpoint for {}", getUserSessionId(), sender.getUserSessionId());
        sender.getOutgoingWebRtcPeer().connect(incomingEndpoint);

        return incomingEndpoint;
    }

    public void cancelVideoFrom(final UserSession sender) {
        this.cancelVideoFrom(sender.getUserSessionId());
    }

    // TODO modifying
    public void cancelVideoFrom(final String senderName) {
        log.debug("PARTICIPANT {}: canceling video reception from {}", getUserSessionId(), senderName);
        final WebRtcEndpoint incoming = incomingMediaMap.remove(senderName);

        log.debug("PARTICIPANT {}: removing endpoint for {}", getUserSessionId(), senderName);
        incoming.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released successfully incoming EP for {}", getUserSessionId(), senderName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release incoming EP for {}", getUserSessionId(), senderName);
            }
        });
    }

    // TODO modifying
    @Override
    public void close() {
        log.debug("PARTICIPANT {}: Releasing resources", getUserSessionId());
        for (final String remoteParticipantName : incomingMediaMap.keySet()) {
            log.trace("PARTICIPANT {}: Released incoming EP for {}", getUserSessionId(), remoteParticipantName);
            final WebRtcEndpoint ep = incomingMediaMap.get(remoteParticipantName);
            ep.release(new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("PARTICIPANT {}: Released successfully incoming EP for {}", getUserSessionId(), remoteParticipantName);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("PARTICIPANT {}: Could not release incoming EP for {}", getUserSessionId(),
                            remoteParticipantName);
                }
            });
        }

        outgoingMedia.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released outgoing EP", getUserSessionId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("USER {}: Could not release outgoing EP", getUserSessionId());
            }
        });
    }

    // send message to this user
    public void sendMessage(Typed message) {
        log.debug("USER {}: Sending message {}", getUserSessionId(), message);

        chatRequestService.sendToWebsocketForSession(userSessionId, message);
    }

    public void addCandidate(IceCandidate candidate, String name) {
        if (getUserSessionId().compareTo(name) == 0) {
            outgoingMedia.addIceCandidate(candidate);
        } else {
            WebRtcEndpoint webRtc = incomingMediaMap.get(name);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

}

// one
class OutgoingWebRtcEndpointIceCandidateFoundListener implements EventListener<IceCandidateFoundEvent> {
    private final String userSessionId;
    private final ChatRequestService chatRequestService;

    OutgoingWebRtcEndpointIceCandidateFoundListener(String userSessionId, ChatRequestService chatRequestService) {
        this.userSessionId = userSessionId;
        this.chatRequestService = chatRequestService;
    }


    @Override
    public void onEvent(IceCandidateFoundEvent event) {
        chatRequestService.sendToWebsocketForSession(userSessionId, new IceCandidateDto(userSessionId, event.getCandidate()));
    }
}

// several
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
        chatRequestService.sendToWebsocketForSession(userSessionId, new IceCandidateDto(otherUserSessionId, event.getCandidate()));
    }

}