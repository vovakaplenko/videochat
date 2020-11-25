package name.nkonev.video.dto.out;

import java.util.List;

public class ExistsParticipantsDto extends Typed {
    public ExistsParticipantsDto() {
    }

    public ExistsParticipantsDto(List<String> participantSessions) {
        this.participantSessions = participantSessions;
    }

    @Override
    public String getType() {
        return "existingParticipants";
    }

    private List<String> participantSessions;

    public List<String> getParticipantSessions() {
        return participantSessions;
    }

    public void setParticipantSessions(List<String> participantSessions) {
        this.participantSessions = participantSessions;
    }
}
