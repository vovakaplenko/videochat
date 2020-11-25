package name.nkonev.video.dto.in;

import java.util.Objects;

public class OnIceCandidateDto extends AuthData {
    private InternalIceCandidateDto candidate;
    private String fromUserSessionId;

    public OnIceCandidateDto() {
    }

    public OnIceCandidateDto(String userSessionId, Long roomId, InternalIceCandidateDto candidate, String fromUserSessionId) {
        this.candidate = candidate;
        this.fromUserSessionId = fromUserSessionId;
        setUserSessionId(userSessionId);
        setRoomId(roomId);
    }

    public InternalIceCandidateDto getCandidate() {
        return candidate;
    }

    public void setCandidate(InternalIceCandidateDto candidate) {
        this.candidate = candidate;
    }

    public String getFromUserSessionId() {
        return fromUserSessionId;
    }

    public void setFromUserSessionId(String fromUserSessionId) {
        this.fromUserSessionId = fromUserSessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OnIceCandidateDto that = (OnIceCandidateDto) o;
        return Objects.equals(candidate, that.candidate) &&
                Objects.equals(fromUserSessionId, that.fromUserSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), candidate, fromUserSessionId);
    }


    public static class InternalIceCandidateDto {
        private String candidate;
        private String sdpMid;
        private int sdpMLineIndex;

        public InternalIceCandidateDto() {
        }

        public InternalIceCandidateDto(String candidate, String sdpMid, int sdpMLineIndex) {
            this.candidate = candidate;
            this.sdpMid = sdpMid;
            this.sdpMLineIndex = sdpMLineIndex;
        }

        public String getCandidate() {
            return candidate;
        }

        public void setCandidate(String candidate) {
            this.candidate = candidate;
        }

        public String getSdpMid() {
            return sdpMid;
        }

        public void setSdpMid(String sdpMid) {
            this.sdpMid = sdpMid;
        }

        public int getSdpMLineIndex() {
            return sdpMLineIndex;
        }

        public void setSdpMLineIndex(int sdpMLineIndex) {
            this.sdpMLineIndex = sdpMLineIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InternalIceCandidateDto that = (InternalIceCandidateDto) o;
            return sdpMLineIndex == that.sdpMLineIndex &&
                    Objects.equals(candidate, that.candidate) &&
                    Objects.equals(sdpMid, that.sdpMid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(candidate, sdpMid, sdpMLineIndex);
        }
    }
}

