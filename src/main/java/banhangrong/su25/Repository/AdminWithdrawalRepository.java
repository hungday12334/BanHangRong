package banhangrong.su25.Repository;
import banhangrong.su25.Entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminWithdrawalRepository extends JpaRepository<WithdrawalRequest,Long> {

    List<WithdrawalRequest> status(String status);

    long countByStatusIgnoreCase(String status);
}
