package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    // Find user by username (used when Principal.getName() returns username)
    Optional<Users> findByUsername(String username);
}
