package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {
    // Có thể thêm các phương thức truy vấn tuỳ ý ở đây

    boolean existsByUsername(String userName);
    boolean existsByEmail(String email);
    Long countByUserType(String userType);
}
