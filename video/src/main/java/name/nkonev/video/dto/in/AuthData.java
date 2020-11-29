package name.nkonev.video.dto.in;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JoinRoomDto.class, name = "joinRoom"),
        @JsonSubTypes.Type(value = NotifyAboutJoinRoomDto.class, name = "notifyAboutJoin"),
        @JsonSubTypes.Type(value = LeaveRoomDto.class, name = "leaveRoom"),
        @JsonSubTypes.Type(value = OnIceCandidateDto.class, name = "onIceCandidate"),
        @JsonSubTypes.Type(value = ReceiveVideoFromDto.class, name = "receiveVideoFrom")
})
public abstract class AuthData {
    protected String userSessionId;
    protected Long roomId;

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthData authData = (AuthData) o;
        return Objects.equals(userSessionId, authData.userSessionId) &&
                Objects.equals(roomId, authData.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userSessionId, roomId);
    }
}
