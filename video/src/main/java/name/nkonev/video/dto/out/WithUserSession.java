package name.nkonev.video.dto.out;

public abstract class WithUserSession extends Typed {
    protected String userSessionId;

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }
}
