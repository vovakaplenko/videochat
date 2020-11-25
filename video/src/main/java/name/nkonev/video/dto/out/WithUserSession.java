package name.nkonev.video.dto.out;

import java.util.Objects;

public abstract class WithUserSession extends Typed {
    protected String userSessionId;

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WithUserSession that = (WithUserSession) o;
        return Objects.equals(userSessionId, that.userSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userSessionId);
    }
}
