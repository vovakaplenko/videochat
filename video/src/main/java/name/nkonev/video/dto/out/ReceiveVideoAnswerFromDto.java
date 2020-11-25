package name.nkonev.video.dto.out;

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
}
