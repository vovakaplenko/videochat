package name.nkonev.video.dto.out;

public class NewParticipantArrivedDto extends WithUserSession{

    public NewParticipantArrivedDto() {}

    public NewParticipantArrivedDto(String userSessionId) {
        setUserSessionId(userSessionId);
    }

    @Override
    public String getType() {
        return "newParticipantArrived";
    }
}
