package banhangrong.su25.Repository;

import banhangrong.su25.Entity.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT w FROM WithdrawalRequest w\n" +
            "WHERE w.userId = :userId\n" +
            "AND (:status IS NULL OR w.status = :status)\n" +
            "AND (:fromTime IS NULL OR w.createdAt >= :fromTime)\n" +
            "AND (:toTime IS NULL OR w.createdAt <= :toTime)\n" +
            "AND (:minAmount IS NULL OR w.amount >= :minAmount)\n" +
            "AND (:maxAmount IS NULL OR w.amount <= :maxAmount)")
    Page<WithdrawalRequest> search(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
