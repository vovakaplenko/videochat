package name.nkonev.users.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.validation.constraints.NotEmpty;
import name.nkonev.users.ApiConstants;

/**
 * Created by nik on 22.06.17.
 * Contains public information
 */
public class UserAccountDTO implements Serializable {
    private static final long serialVersionUID = -5796134399691582320L;

    private Long id;

    @NotEmpty
    protected String login;

    private String avatar;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern= ApiConstants.DATE_FORMAT)
    private LocalDateTime lastLoginDateTime;

    private OauthIdentifiersDTO oauthIdentifiers = new OauthIdentifiersDTO();

    public static class DataDTO {
        private boolean enabled;
        private boolean expired;
        private boolean locked;
        private UserRole role;

        public DataDTO(boolean enabled, boolean expired, boolean locked, UserRole role) {
            this.enabled = enabled;
            this.expired = expired;
            this.locked = locked;
            this.role = role;
        }

        public DataDTO() { }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }


    public UserAccountDTO(Long id, String login, String avatar, LocalDateTime lastLoginDateTime, OauthIdentifiersDTO oauthIdentifiers) {
        this.id = id;
        this.login = login;
        this.avatar = avatar;
        this.lastLoginDateTime = lastLoginDateTime;
        if (oauthIdentifiers!=null) {
            this.oauthIdentifiers = oauthIdentifiers;
        }
    }


    public UserAccountDTO() { }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getLastLoginDateTime() {
        return lastLoginDateTime;
    }

    public void setLastLoginDateTime(LocalDateTime lastLoginDateTime) {
        this.lastLoginDateTime = lastLoginDateTime;
    }

    public OauthIdentifiersDTO getOauthIdentifiers() {
        return oauthIdentifiers;
    }

    public void setOauthIdentifiers(OauthIdentifiersDTO oauthIdentifiers) {
        this.oauthIdentifiers = oauthIdentifiers;
    }
}
