package name.nkonev.users.service;

import name.nkonev.users.Constants;
import name.nkonev.users.entity.jdbc.UserAccount;
import name.nkonev.users.repository.jdbc.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserDeleteService {
    @Autowired
    private FutureUserDetailsService blogUserDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    public long deleteUser(long userId) {
        blogUserDetailsService.killSessions(userId);
        UserAccount deleted = userAccountRepository.findByUsername(Constants.DELETED).orElseThrow();
        userAccountRepository.deleteById(userId);

        return userAccountRepository.count();
    }

}
