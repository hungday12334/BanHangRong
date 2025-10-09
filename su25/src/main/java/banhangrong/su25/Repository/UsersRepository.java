package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    List<Users> findByUserType(String userType);

    @Query("SELECT u FROM Users u WHERE u.userType = 'SELLER' AND u.isActive = true")
    List<Users> findAllActiveSellers();

    boolean existsByEmailAndUserType(String email, String userType);
}
