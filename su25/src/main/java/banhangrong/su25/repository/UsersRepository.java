package banhangrong.su25.repository;

import banhangrong.su25.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {
    // Có thể thêm các phương thức truy vấn tuỳ ý ở đây
}
