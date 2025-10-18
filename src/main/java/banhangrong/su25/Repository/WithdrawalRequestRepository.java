package banhangrong.su25.Repository;

import banhangrong.su25.Entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
