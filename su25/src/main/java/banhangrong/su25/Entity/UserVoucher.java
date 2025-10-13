package banhangrong.su25.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "voucher_id"})
})
public class UserVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userVoucherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(nullable = false)
    private Boolean isUsed = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @Column(length = 100)
    private String sentReason;

    @Column(columnDefinition = "JSON")
    private String contextData;

    // Constructors
    public UserVoucher() {}

    public UserVoucher(Users user, Voucher voucher, String sentReason, String contextData) {
        this.user = user;
        this.voucher = voucher;
        this.sentReason = sentReason;
        this.contextData = contextData;
    }

    // Business Methods
    public void markAsUsed() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return !isUsed && voucher.isValid();
    }

    // Getters and Setters
    public Long getUserVoucherId() { return userVoucherId; }
    public void setUserVoucherId(Long userVoucherId) { this.userVoucherId = userVoucherId; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getSentReason() { return sentReason; }
    public void setSentReason(String sentReason) { this.sentReason = sentReason; }

    public String getContextData() { return contextData; }
    public void setContextData(String contextData) { this.contextData = contextData; }
}