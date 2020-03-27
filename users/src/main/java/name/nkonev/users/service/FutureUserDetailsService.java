package name.nkonev.users.service;

import name.nkonev.users.entity.jdbc.UserAccount;
import name.nkonev.users.repository.jdbc.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FutureUserDetailsService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    public void killSessions(long userId){
    }

    public void refreshUserDetails(UserAccount exists) {

    }

    public UserAccount getUserAccount(long userId){
        return userAccountRepository.findById(userId).orElseThrow();
    }
}
