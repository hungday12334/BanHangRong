package banhangrong.su25.Repository;

import banhangrong.su25.Entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByUserId(Long userId);
    Optional<BankAccount> findFirstByUserIdAndIsDefaultTrue(Long userId);
}
