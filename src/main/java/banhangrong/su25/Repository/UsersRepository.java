package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Search by username or email (contains, case-insensitive) with optional userType filter
    @Query("SELECT u FROM Users u WHERE (:type IS NULL OR LOWER(u.userType) = LOWER(:type)) AND ( :q IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) )")
    Page<Users> searchUsers(@Param("type") String type, @Param("q") String q, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.email = :email, u.isEmailVerified = false WHERE u.userId = :userId")
    int updateEmailAndUnverify(@Param("userId") Long userId, @Param("email") String email);
}
