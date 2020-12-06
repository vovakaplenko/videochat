package name.nkonev.video.dto.out;

public class ParticipantLeftDto extends WithUserSession {
    public ParticipantLeftDto() {
    }

    public ParticipantLeftDto(String userSessionDto) {
        setUserSessionId(userSessionDto);
    }

    @Override
    public String getType() {
        return "videoParticipantLeft";
    }
}
