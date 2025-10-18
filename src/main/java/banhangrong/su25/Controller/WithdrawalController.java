package banhangrong.su25.Controller;

import banhangrong.su25.Entity.BankAccount;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.WithdrawalRequest;
import banhangrong.su25.Util.SecurityUtil;
import banhangrong.su25.service.WithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/seller/withdraw")
public class WithdrawalController {
    private final WithdrawalService withdrawalService;
    private final SecurityUtil securityUtil;

    public WithdrawalController(WithdrawalService withdrawalService, SecurityUtil securityUtil) {
        this.withdrawalService = withdrawalService;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/summary")
    @ResponseBody
    public ResponseEntity<?> summary() {
        Optional<Users> userOpt = securityUtil.getCurrentUser();
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("Unauthorized");
        Users user = userOpt.get();
        Map<String,Object> res = new HashMap<>();
        res.put("balance", user.getBalance());
        res.put("accounts", withdrawalService.listBankAccounts(user.getUserId()));
        res.put("history", withdrawalService.history(user.getUserId()));
        res.put("feePercent", new BigDecimal("2.00"));
        return ResponseEntity.ok(res);
    }

    public record CreateWithdrawPayload(Long bankAccountId, BigDecimal amount, Boolean withdrawAll) {}
    public record CreateBankPayload(String bankName, String bankCode, String accountNumber, String accountHolderName, String branch, Boolean makeDefault) {}

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody CreateWithdrawPayload payload) {
        Optional<Users> userOpt = securityUtil.getCurrentUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Users user = userOpt.get();

        BigDecimal amount = payload.withdrawAll() != null && payload.withdrawAll()
                ? user.getBalance()
                : payload.amount();

        try {
            WithdrawalRequest wr = withdrawalService.createWithdrawal(user.getUserId(), payload.bankAccountId(), amount, new BigDecimal("2.00"));
            Map<String, Object> res = new HashMap<>();
            res.put("id", wr.getWithdrawalId());
            res.put("status", wr.getStatus());
            res.put("fee", wr.getFeeAmount());
            res.put("net", wr.getNetAmount());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/bank-account")
    @ResponseBody
    public ResponseEntity<?> addBank(@RequestBody CreateBankPayload payload){
        Optional<Users> userOpt = securityUtil.getCurrentUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Users user = userOpt.get();
        try {
            BankAccount saved = withdrawalService.createBankAccount(
                    user.getUserId(),
                    payload.bankName(), payload.bankCode(), payload.accountNumber(),
                    payload.accountHolderName(), payload.branch(), Boolean.TRUE.equals(payload.makeDefault())
            );
            Map<String, Object> res = new HashMap<>();
            res.put("bankAccountId", saved.getBankAccountId());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
