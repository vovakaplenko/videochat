package name.nkonev.video.dto.in;

import java.util.Objects;

public abstract class AuthData implements EmbeddedPayload {
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
