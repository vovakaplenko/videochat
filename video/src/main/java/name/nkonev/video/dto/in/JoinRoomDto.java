package name.nkonev.video.dto.in;

public class JoinRoomDto extends AuthData {
    public JoinRoomDto() {
    }

    public JoinRoomDto(String userSessionId, Long roomId) {
        setRoomId(roomId);
        setUserSessionId(userSessionId);
    }

}
