package banhangrong.su25.service;

import banhangrong.su25.Entity.BankAccount;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.WithdrawalRequest;
import banhangrong.su25.Repository.BankAccountRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WithdrawalService {
    private final WithdrawalRequestRepository withdrawalRepo;
    private final BankAccountRepository bankRepo;
    private final UsersRepository usersRepo;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRepo, BankAccountRepository bankRepo, UsersRepository usersRepo) {
        this.withdrawalRepo = withdrawalRepo;
        this.bankRepo = bankRepo;
        this.usersRepo = usersRepo;
    }

    public List<WithdrawalRequest> history(Long userId) {
        return withdrawalRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<BankAccount> listBankAccounts(Long userId) {
        return bankRepo.findByUserId(userId);
    }

    public Optional<BankAccount> getDefaultBank(Long userId) {
        return bankRepo.findFirstByUserIdAndIsDefaultTrue(userId);
    }

    @Transactional
    public BankAccount createBankAccount(Long userId, String bankName, String bankCode,
                                         String accountNumber, String accountHolderName,
                                         String branch, boolean makeDefault) {
        if (bankName == null || accountNumber == null || accountHolderName == null) {
            throw new IllegalArgumentException("Thông tin ngân hàng không hợp lệ");
        }
        if (makeDefault) {
            // unset existing defaults
            List<BankAccount> list = bankRepo.findByUserId(userId);
            for (BankAccount b : list) {
                if (Boolean.TRUE.equals(b.getIsDefault())) {
                    b.setIsDefault(false);
                }
            }
            bankRepo.saveAll(list);
        }
        BankAccount b = new BankAccount();
        b.setUserId(userId);
        b.setBankName(bankName);
        b.setBankCode(bankCode);
        b.setAccountNumber(accountNumber);
        b.setAccountHolderName(accountHolderName);
        b.setBranch(branch);
        b.setIsDefault(makeDefault);
        return bankRepo.save(b);
    }

    @Transactional
    public WithdrawalRequest createWithdrawal(Long userId, Long bankAccountId, BigDecimal requestedAmount, BigDecimal feePercentOverride) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải > 0");
        }

        Users user = usersRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        BankAccount bank = bankRepo.findById(bankAccountId).orElseThrow(() -> new IllegalArgumentException("Tài khoản ngân hàng không tồn tại"));

        if (!bank.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không thể dùng tài khoản ngân hàng của người khác");
        }

        BigDecimal balance = Optional.ofNullable(user.getBalance()).orElse(BigDecimal.ZERO);
        if (requestedAmount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Số dư không đủ");
        }

        BigDecimal feePercent = feePercentOverride != null ? feePercentOverride : new BigDecimal("2.00");
        BigDecimal feeAmount = requestedAmount.multiply(feePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = requestedAmount.subtract(feeAmount);

        // Deduct immediately to lock funds
        user.setBalance(balance.subtract(requestedAmount));
        usersRepo.save(user);

        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setUserId(userId);
        wr.setBankAccountId(bankAccountId);
        wr.setAmount(requestedAmount.setScale(2, RoundingMode.HALF_UP));
        wr.setFeePercent(feePercent.setScale(2, RoundingMode.HALF_UP));
        wr.setFeeAmount(feeAmount);
        wr.setNetAmount(netAmount);
        wr.setStatus("PENDING");
        wr.setCreatedAt(LocalDateTime.now());
        wr.setUpdatedAt(LocalDateTime.now());
        return withdrawalRepo.save(wr);
    }

    @Transactional
    public WithdrawalRequest markCompleted(Long withdrawalId, String providerRef) {
        WithdrawalRequest wr = withdrawalRepo.findById(withdrawalId).orElseThrow(() -> new IllegalArgumentException("Lệnh rút không tồn tại"));
        wr.setStatus("COMPLETED");
        wr.setProviderReference(providerRef);
        wr.setProcessedAt(LocalDateTime.now());
        wr.setUpdatedAt(LocalDateTime.now());
        return withdrawalRepo.save(wr);
    }

    @Transactional
    public WithdrawalRequest markFailed(Long withdrawalId, String reason) {
        WithdrawalRequest wr = withdrawalRepo.findById(withdrawalId).orElseThrow(() -> new IllegalArgumentException("Lệnh rút không tồn tại"));
        // refund funds
        Users user = usersRepo.findById(wr.getUserId()).orElseThrow();
        BigDecimal balance = Optional.ofNullable(user.getBalance()).orElse(BigDecimal.ZERO);
        user.setBalance(balance.add(wr.getAmount()));
        usersRepo.save(user);

        wr.setStatus("FAILED");
        wr.setNote(reason);
        wr.setProcessedAt(LocalDateTime.now());
        wr.setUpdatedAt(LocalDateTime.now());
        return withdrawalRepo.save(wr);
    }
}
