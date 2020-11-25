package name.nkonev.video.dto.in;

public class ReceiveVideoFromDto {
    private String senderSessionId;
    private String sdpOffer;

    public ReceiveVideoFromDto() {
    }

    public ReceiveVideoFromDto(String senderSessionId, String sdpOffer) {
        this.senderSessionId = senderSessionId;
        this.sdpOffer = sdpOffer;
    }

    public String getSenderSessionId() {
        return senderSessionId;
    }

    public void setSenderSessionId(String senderSessionId) {
        this.senderSessionId = senderSessionId;
    }

    public String getSdpOffer() {
        return sdpOffer;
    }

    public void setSdpOffer(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }
}
