package name.nkonev.video.dto.in;

public class LeaveRoomDto extends AuthData {
    public LeaveRoomDto() {
    }

    public LeaveRoomDto(String userSessionId, Long roomId) {
        setRoomId(roomId);
        setUserSessionId(userSessionId);
    }

}
