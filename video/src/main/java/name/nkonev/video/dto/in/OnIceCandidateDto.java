package name.nkonev.video.dto.in;

public class OnIceCandidateDto {
    private InternalIceCandidateDto candidate;
    private String fromUserSessionId;

    public OnIceCandidateDto() {
    }

    public OnIceCandidateDto(InternalIceCandidateDto candidate, String fromUserSessionId) {
        this.candidate = candidate;
        this.fromUserSessionId = fromUserSessionId;
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
    }
}

