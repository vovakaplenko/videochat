package name.nkonev.video.dto.out;

import org.kurento.client.IceCandidate;

public class IceCandidateDto extends WithUserSession {
    public IceCandidateDto(String userSessionId, IceCandidate candidate) {
        this.candidate = candidate;
        setUserSessionId(userSessionId);
    }

    public IceCandidateDto() {
    }

    @Override
    public String getType() {
        return "iceCandidate";
    }

    private IceCandidate candidate;

    public IceCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(IceCandidate candidate) {
        this.candidate = candidate;
    }
}
