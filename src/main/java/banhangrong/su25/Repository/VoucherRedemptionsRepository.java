package banhangrong.su25.Repository;

import banhangrong.su25.Entity.VoucherRedemptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRedemptionsRepository extends JpaRepository<VoucherRedemptions, Long> {
    List<VoucherRedemptions> findByVoucherId(Long voucherId);
}