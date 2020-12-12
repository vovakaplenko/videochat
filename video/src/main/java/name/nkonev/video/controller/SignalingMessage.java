package name.nkonev.video.controller;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HelloMessage.class, name = "videoHello"),
        @JsonSubTypes.Type(value = ByeMessage.class, name = "videoBye"),
        @JsonSubTypes.Type(value = CandidateMessage.class, name = "videoCandidate"),
        @JsonSubTypes.Type(value = OfferMessage.class, name = "videoOffer"),
        @JsonSubTypes.Type(value = AnswerMessage.class, name = "videoAnswer")
})
public abstract class SignalingMessage {
    public Long fromUserId;
    public Long chatId;
}