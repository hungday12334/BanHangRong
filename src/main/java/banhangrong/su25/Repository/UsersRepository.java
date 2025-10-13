package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Long countByUserType(String userType);

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.email = :email, u.isEmailVerified = false WHERE u.userId = :userId")
    int updateEmailAndUnverify(@Param("userId") Long userId, @Param("email") String email);
}
