package name.nkonev.video.dto.in;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JoinRoomDto.class, name = "joinRoomDto"),
        @JsonSubTypes.Type(value = LeaveRoomDto.class, name = "leaveRoomDto"),
        @JsonSubTypes.Type(value = OnIceCandidateDto.class, name = "onIceCandidateDto"),
        @JsonSubTypes.Type(value = ReceiveVideoFromDto.class, name = "receiveVideoFromDto")
})
public interface EmbeddedPayload {
}
