package name.nkonev.users.repository.redis;

import name.nkonev.users.entity.redis.UserConfirmationToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConfirmationTokenRepository extends
    CrudRepository<UserConfirmationToken, String> {
}
