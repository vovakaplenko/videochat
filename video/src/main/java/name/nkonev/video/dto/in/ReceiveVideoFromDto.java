package name.nkonev.video.dto.in;

import java.util.Objects;

public class ReceiveVideoFromDto extends AuthData {
    private String senderSessionId;
    private String sdpOffer;

    public ReceiveVideoFromDto() {
    }

    public ReceiveVideoFromDto(String userSessionId, Long roomId, String senderSessionId, String sdpOffer) {
        this.senderSessionId = senderSessionId;
        this.sdpOffer = sdpOffer;
        setUserSessionId(userSessionId);
        setRoomId(roomId);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReceiveVideoFromDto that = (ReceiveVideoFromDto) o;
        return Objects.equals(senderSessionId, that.senderSessionId) &&
                Objects.equals(sdpOffer, that.sdpOffer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), senderSessionId, sdpOffer);
    }
}