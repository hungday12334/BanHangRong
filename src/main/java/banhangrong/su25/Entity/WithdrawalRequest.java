package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_id")
    private Long withdrawalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(nullable = false)
    private BigDecimal amount; // requested amount

    @Column(name = "fee_percent", nullable = false)
    private BigDecimal feePercent; // e.g., 2.00

    @Column(name = "fee_amount", nullable = false)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED

    private String note;
    @Column(name = "payout_provider")
    private String payoutProvider;
    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Long getWithdrawalId() { return withdrawalId; }
    public void setWithdrawalId(Long withdrawalId) { this.withdrawalId = withdrawalId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeePercent() { return feePercent; }
    public void setFeePercent(BigDecimal feePercent) { this.feePercent = feePercent; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPayoutProvider() { return payoutProvider; }
    public void setPayoutProvider(String payoutProvider) { this.payoutProvider = payoutProvider; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
