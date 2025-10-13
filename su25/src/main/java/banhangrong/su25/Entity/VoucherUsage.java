package banhangrong.su25.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_usage")
public class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id", nullable = false)
    private UserVoucher userVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedAt;

    // Constructors
    public VoucherUsage() {}

    public VoucherUsage(UserVoucher userVoucher, Orders order, BigDecimal discountAmount) {
        this.userVoucher = userVoucher;
        this.order = order;
        this.discountAmount = discountAmount;
    }

    // Getters and Setters
    public Long getUsageId() { return usageId; }
    public void setUsageId(Long usageId) { this.usageId = usageId; }

    public UserVoucher getUserVoucher() { return userVoucher; }
    public void setUserVoucher(UserVoucher userVoucher) { this.userVoucher = userVoucher; }

    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}