package name.nkonev.users.repository.redis;

import java.util.UUID;
import name.nkonev.users.entity.redis.PasswordResetToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, UUID> {

}
