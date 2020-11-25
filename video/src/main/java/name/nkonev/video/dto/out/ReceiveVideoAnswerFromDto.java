package name.nkonev.video.dto.out;

import java.util.Objects;

public class ReceiveVideoAnswerFromDto extends WithUserSession {
    public ReceiveVideoAnswerFromDto(String userSessionId, String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
        setUserSessionId(userSessionId);
    }

    public ReceiveVideoAnswerFromDto() {
    }

    @Override
    public String getType() {
        return "receiveVideoAnswer";
    }

    private String sdpAnswer;

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public void setSdpAnswer(String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReceiveVideoAnswerFromDto that = (ReceiveVideoAnswerFromDto) o;
        return Objects.equals(sdpAnswer, that.sdpAnswer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sdpAnswer);
    }
}
