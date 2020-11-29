package name.nkonev.video.dto.in;

public class NotifyAboutJoinRoomDto extends AuthData {
    public NotifyAboutJoinRoomDto() {
    }

    public NotifyAboutJoinRoomDto(String userSessionId, Long roomId) {
        setRoomId(roomId);
        setUserSessionId(userSessionId);
    }

}
