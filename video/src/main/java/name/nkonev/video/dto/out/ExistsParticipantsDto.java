package name.nkonev.video.dto.out;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExistsParticipantsDto that = (ExistsParticipantsDto) o;
        return Objects.equals(participantSessions, that.participantSessions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participantSessions);
    }
}
