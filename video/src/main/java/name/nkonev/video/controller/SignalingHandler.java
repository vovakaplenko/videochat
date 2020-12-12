package name.nkonev.video.controller;

import com.fasterxml.jackson.databind.JsonNode;
import name.nkonev.video.dto.out.ReceiveVideoAnswerFromDto;
import name.nkonev.video.service.ChatRequestService;
import name.nkonev.video.service.UserSession;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.PipedInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class HelloMessage extends SignalingMessage {

}
class ByeMessage extends SignalingMessage {

}

class CandidateMessage extends SignalingMessage {
    public Integer sdpMLineIndex;
    public String sdpMid;
    public String candidate;

    public Long toUserId;

    public CandidateMessage() {
    }

    public CandidateMessage(Integer sdpMLineIndex, String sdpMid, String candidate, Long toUserId) {
        this.sdpMLineIndex = sdpMLineIndex;
        this.sdpMid = sdpMid;
        this.candidate = candidate;
        this.toUserId = toUserId;
    }
}
class OfferMessage extends SignalingMessage {
    public JsonNode value; // sessionDescription

    public Long toUserId;

    public OfferMessage() {
    }

    public OfferMessage(JsonNode value, Long toUserId) {
        this.value = value;
        this.toUserId = toUserId;
    }
}
class AnswerMessage extends SignalingMessage {
    public JsonNode value; // sessionDescription

    public Long toUserId;

    public AnswerMessage() {
    }

    public AnswerMessage(JsonNode value, Long toUserId) {
        this.value = value;
        this.toUserId = toUserId;
    }
}

class ChatRoom {
    private Map<Long, Participant> participants = new ConcurrentHashMap<>();
    private final MediaPipeline pipeline;
    private final ChatRequestService chatRequestService;
    private final Long id;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatRoom.class);

    public ChatRoom(MediaPipeline pipeline, ChatRequestService chatRequestService, Long id) {
        this.pipeline = pipeline;
        this.chatRequestService = chatRequestService;
        this.id = id;
    }

    public void addParticipant(Participant participant) {
        LOGGER.info("Adding participant {} to chatRoom {}", participant.getId(), id);
        participants.values().forEach(existingParticipant -> {
            existingParticipant.addIncomingFrom(participant);
        });

        participants.put(participant.getId(), participant);
    }

    public void removeParticipant(Long participantId) {
        participants.remove(participantId);
        participants.values().forEach(existingParticipant -> {
            existingParticipant.removeIncomingFrom(participantId);
        });
    }

    public void broadcast(Long exceptParticipantId, SignalingMessage broadcastMessage) {
        for (Participant p: participants.values()) {
            if (p.getId().equals(exceptParticipantId)) {
                continue;
            }
            chatRequestService.send(p.getId(), broadcastMessage);
        }
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    public void destroy() {
        pipeline.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                LOGGER.info("ROOM {}: Released Pipeline", id);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                LOGGER.warn("ROOM {}: Could not release Pipeline", id);
            }
        });
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public Participant getParticipant(Long userId) {
        return participants.get(userId);
    }

    public List<Participant> getParticipants(Long exceptUserId) {
        return participants.values().stream().filter(participant -> !participant.getId().equals(exceptUserId)).collect(Collectors.toList());
    }
}

class Participant {
    private final Long id;
    private final WebRtcEndpoint outgoingMedia;
    private final ChatRequestService chatRequestService;
    private final ConcurrentMap<Long, WebRtcEndpoint> incomingMediaMap = new ConcurrentHashMap<>();
    private final MediaPipeline roomMediaPipeline;

    private static final Logger LOGGER = LoggerFactory.getLogger(Participant.class);

    public Participant(Long id, MediaPipeline roomMediaPipeline, ChatRequestService chatRequestService) {
        this.id = id;
        this.roomMediaPipeline = roomMediaPipeline;
        this.chatRequestService = chatRequestService;
        this.outgoingMedia = new WebRtcEndpoint.Builder(roomMediaPipeline).build();
        this.outgoingMedia.addIceCandidateFoundListener(new MyOutgoingWebRtcEndpointIceCandidateFoundListener(id, chatRequestService));
    }

    public Long getId() {
        return id;
    }

    public void addIncomingFrom(Participant newParticipant) {
        LOGGER.info("Adding for participant {} incoming from participant {}", id, newParticipant.getId());
        WebRtcEndpoint incomingEndpoint = new WebRtcEndpoint.Builder(roomMediaPipeline).build();
        // here I receive messages from other newParticipant
        incomingEndpoint.addIceCandidateFoundListener(new MyIncomingWebRtcEndpointIceCandidateFoundListener(id, chatRequestService));

        incomingMediaMap.put(newParticipant.getId(), incomingEndpoint);
        newParticipant.getOutgoingWebRtcPeer().connect(incomingEndpoint);
    }

    private WebRtcEndpoint getOutgoingWebRtcPeer() {
        return outgoingMedia;
    }

    public void removeIncomingFrom(Long participantId) {
        LOGGER.info("Removing for participant {} incoming from participant {}", id, participantId);
        final WebRtcEndpoint webRtcEndpoint = incomingMediaMap.get(participantId);
        if (webRtcEndpoint!= null) {
            webRtcEndpoint.release(new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    LOGGER.trace("PARTICIPANT {}: Released successfully incoming EP for {}", participantId, id);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    LOGGER.warn("PARTICIPANT {}: Could not release incoming EP for {}",  participantId, id);
                }
            });
        }
    }

    public void addIceToOutgoingMedia(IceCandidate cand) {
        outgoingMedia.addIceCandidate(cand);
    }

    public void addIceToIncomingFor(IceCandidate cand, Participant fromParticipant) {
        final WebRtcEndpoint webRtcEndpoint = incomingMediaMap.get(fromParticipant.getId());
        if (webRtcEndpoint!=null) {
            webRtcEndpoint.addIceCandidate(cand);
        }
    }

    public void processOffer(OfferMessage offerMessage) {
        final Long toUserId = offerMessage.toUserId;
        final WebRtcEndpoint webRtcEndpoint = incomingMediaMap.get(toUserId);
        if (webRtcEndpoint != null) {

        }
    }

//    private WebRtcEndpoint getEndpointForUser(final UserSession sender) {
//        if (sender.getUserSessionId().equals(id)) {
//            LOGGER.debug("PARTICIPANT {}: configuring loopback", getUserSessionId());
//            return outgoingMedia;
//        }
//
//        WebRtcEndpoint incomingEndpoint = incomingMediaMap.get(sender.getUserSessionId());
//        if (incomingEndpoint == null) {
//            incomingEndpoint = new WebRtcEndpoint.Builder(roomMediaPipeline).build();
//
//            incomingEndpoint.addIceCandidateFoundListener(new IncomingWebRtcEndpointIceCandidateFoundListener(userSessionId, sender.getUserSessionId(), chatRequestService));
//
//            incomingMediaMap.put(sender.getUserSessionId(), incomingEndpoint);
//        }
//
//        sender.getOutgoingWebRtcPeer().connect(incomingEndpoint);
//
//        return incomingEndpoint;
//    }

}

@RestController
public class SignalingHandler {

    public static final String INVOKE = "/invoke";

    private static final Logger LOGGER = LoggerFactory.getLogger(SignalingHandler.class);

    @Autowired
    private KurentoClient kurento;

    @Autowired
    private ChatRequestService chatRequestService;

    private Map<Long, ChatRoom> rooms = new ConcurrentHashMap<>();

    @PostMapping(INVOKE)
    public void invoke(@RequestBody SignalingMessage invokeDto) {
        LOGGER.info("Newly Received {}", invokeDto);
        if (invokeDto instanceof HelloMessage) {
            ChatRoom chatRoom = getOrCreateChatRoomIfNeed(invokeDto.chatId);
            Participant participant = new Participant(invokeDto.fromUserId, chatRoom.getPipeline(), chatRequestService);
            chatRoom.addParticipant(participant);
            chatRoom.broadcast(invokeDto.fromUserId, invokeDto);
        } if (invokeDto instanceof ByeMessage) {
            ChatRoom chatRoom = getOrCreateChatRoomIfNeed(invokeDto.chatId);
            chatRoom.removeParticipant(invokeDto.fromUserId);
            chatRoom.broadcast(invokeDto.fromUserId, invokeDto);
            if (chatRoom.isEmpty()) {
                removeChatRoom(invokeDto.chatId);
            }
        } if (invokeDto instanceof CandidateMessage) {
            CandidateMessage candidate = (CandidateMessage) invokeDto;
            IceCandidate cand = new IceCandidate(
                    candidate.candidate,
                    candidate.sdpMid,
                    candidate.sdpMLineIndex
            );
            ChatRoom chatRoom = getOrCreateChatRoomIfNeed(invokeDto.chatId);
            Participant meParticipant = chatRoom.getParticipant(candidate.fromUserId);
            // add for outgoingMedia for me
            meParticipant.addIceToOutgoingMedia(cand);

            // add for incomingMediaMap for others
            List<Participant> participantsExceptMe = chatRoom.getParticipants(candidate.fromUserId);
            participantsExceptMe.forEach(otherParticipant -> {
                otherParticipant.addIceToIncomingFor(cand, meParticipant);
            });
        } if (invokeDto instanceof OfferMessage) {
            OfferMessage offerMessage = (OfferMessage) invokeDto;
            ChatRoom chatRoom = getOrCreateChatRoomIfNeed(invokeDto.chatId);
            Participant meParticipant = chatRoom.getParticipant(offerMessage.fromUserId);
            //incomingMediaMap.get(sender.getUserSessionId());
            meParticipant.processOffer(offerMessage);
        }
    }

    private void removeChatRoom(Long chatId) {
        rooms.get(chatId).destroy();
        rooms.remove(chatId);
    }

    private ChatRoom getOrCreateChatRoomIfNeed(Long chatId) {
        return rooms.computeIfAbsent(chatId, aLong -> {
            LOGGER.info("Creating chatRoom {}", chatId);
            return new ChatRoom(kurento.createMediaPipeline(), chatRequestService, chatId);
        });
    }
}


// one (me -> kurento -> otherParticipant1, otherParticipant2...)
class MyOutgoingWebRtcEndpointIceCandidateFoundListener implements EventListener<IceCandidateFoundEvent> {
    private final Long userId;
    private final ChatRequestService chatRequestService;

    MyOutgoingWebRtcEndpointIceCandidateFoundListener(Long userId, ChatRequestService chatRequestService) {
        this.userId = userId;
        this.chatRequestService = chatRequestService;
    }


    @Override
    public void onEvent(IceCandidateFoundEvent event) {
        final IceCandidate candidate = event.getCandidate();
        chatRequestService.send(userId, new CandidateMessage(candidate.getSdpMLineIndex(), candidate.getSdpMid(), candidate.getCandidate(), userId));
    }
}

// several (otherParticipant1, otherParticipant2... -> kurento -> me)
class MyIncomingWebRtcEndpointIceCandidateFoundListener implements EventListener<IceCandidateFoundEvent> {

    private final Long toUserId;
    private final ChatRequestService chatRequestService;

    MyIncomingWebRtcEndpointIceCandidateFoundListener(Long toUserId, ChatRequestService chatRequestService) {
        this.toUserId = toUserId;
        this.chatRequestService = chatRequestService;
    }

    @Override
    public void onEvent(IceCandidateFoundEvent event) {
        final IceCandidate candidate = event.getCandidate();
        chatRequestService.send(toUserId, new CandidateMessage(candidate.getSdpMLineIndex(), candidate.getSdpMid(), candidate.getCandidate(), toUserId));
    }

}