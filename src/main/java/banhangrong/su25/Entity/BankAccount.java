package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    private String branch;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean aDefault) { isDefault = aDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
