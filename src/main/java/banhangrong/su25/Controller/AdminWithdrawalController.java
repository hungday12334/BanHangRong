package banhangrong.su25.Controller;

import banhangrong.su25.Entity.BankAccount;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.WithdrawalRequest;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
import banhangrong.su25.service.AdminWithdrawalService;
import banhangrong.su25.service.BankAccountService;
import banhangrong.su25.service.UserService;
import banhangrong.su25.service.WithdrawalService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/withdrawal")
public class AdminWithdrawalController {

    @Autowired
    private AdminWithdrawalService withdrawalService;
    @Autowired
    private UserService userService;

    @Autowired
    private BankAccountService bankAccountService;
    @Autowired
    private EmailService emailService;

    @GetMapping()
    public String index(
            Model model,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortOrder", required = false) String sortOrder
    ) {
        List<WithdrawalRequest> withdrawals;
        withdrawals = withdrawalService.fiilter(status, sortBy, sortOrder);
        model.addAttribute("withdrawals", withdrawals);
        model.addAttribute("status", status);
        return "admin/withdrawal-management";
    }


    @GetMapping("/detail")
    public String showCreateScreen(Model model, @RequestParam("id") String withdrawalId, RedirectAttributes redirectAttributes) {
        if (withdrawalId == null || withdrawalId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Withdrawal not found");
            return "redirect:/admin/withdrawal";
        }
        //Lay withdrawal tu database
        WithdrawalRequest withdrawalRequest = null;
        Long id = null;
        try {
            id = Long.parseLong(withdrawalId);
            withdrawalRequest = withdrawalService.findById(id);
            if (withdrawalRequest == null) {
                redirectAttributes.addFlashAttribute("error", "Withdrawal not found");
                return "redirect:/admin/withdrawal";
            }
            model.addAttribute("withdrawalRequest", withdrawalRequest);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Invalid withdrawal ID");
            return "redirect:/admin/withdrawal";
        }


        //Lay thong tin ngan hang
        BankAccount bankAccount;
        try {
            bankAccount = bankAccountService.findById(withdrawalRequest.getBankAccountId());
            if (bankAccount == null) {
                model.addAttribute("error", "Bank account not found");
            } else {
                model.addAttribute("bankAccount", bankAccount);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Invalid bank account ID");
        }
        return "admin/withdrawal-update";
    }

    @PostMapping("/update")
    public String updateStatus(
            @RequestParam("id") String withdrawalId,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {

        if (withdrawalId == null || withdrawalId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Withdrawal ID is required.");
            return "redirect:/admin/withdrawal";
        }

        Long id;
        try {
            id = Long.parseLong(withdrawalId);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid withdrawal ID format.");
            return "redirect:/admin/withdrawal";
        }

        if (status == null || status.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Status is required.");
            return "redirect:/admin/withdrawal/detail?id=" + id;
        }

        WithdrawalRequest withdrawalRequest = withdrawalService.findById(id);
        if (withdrawalRequest == null) {
            redirectAttributes.addFlashAttribute("error", "Withdrawal not found.");
            return "redirect:/admin/withdrawal";
        }

        Users user = userService.findById(withdrawalRequest.getUserId());
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/withdrawal/detail?id=" + id;
        }

        LocalDateTime now = LocalDateTime.now();

        if (!"PENDING".equalsIgnoreCase(withdrawalRequest.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "This withdrawal is not in PENDING status.");
            return "redirect:/admin/withdrawal/detail?id=" + id;
        }
        BankAccount bankAccount = bankAccountService.findById(withdrawalRequest.getBankAccountId());
        String actionResult = "";
        String emailSubject = "";
        String emailMessage = "";
        String amountVND = String.format("%,.0f", withdrawalRequest.getNetAmount());
        if ("Approved".equalsIgnoreCase(status)) {
            withdrawalRequest.setStatus("Approved");
            actionResult = "approved";
            emailSubject = "Withdrawal Request Approved";

            emailMessage = """
        <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden;">
            <div style="background: linear-gradient(135deg, #00A86B, #20c997); padding: 20px; text-align: center; color: white;">
                <h2 style="margin: 0;">Withdrawal Approved</h2>
            </div>
            <div style="padding: 25px; background-color: #fff;">
                <p>Hi <strong>%s</strong>,</p>
                <p>Your withdrawal request has been <strong>APPROVED</strong>.</p>
                <div style="background-color: #f8fff9; border-left: 4px solid #00A86B; padding: 15px; margin: 20px 0;">
                    <p style="margin: 0;"><strong>Amount:</strong> %s VND</p>
                    <p style="margin: 8px 0 0;"><strong>Bank:</strong> %s - %s - %s</p>
                    <p style="margin: 8px 0 0;"><strong>Approved At:</strong> %s</p>
                </div>
                <p>Thank you for using our platform!</p>
            </div>
        </div>
        """.formatted(
                    user.getUsername(),
                    amountVND,
                    bankAccount.getBankName(),
                    bankAccount.getAccountHolderName(),
                    bankAccount.getAccountNumber(),
                    now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

        } else if ("Cancelled".equalsIgnoreCase(status)) {
            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Reason is required for cancellation.");
                return "redirect:/admin/withdrawal/detail?id=" + id;
            }
            withdrawalRequest.setNote(reason);
            withdrawalRequest.setStatus("Cancelled");
            actionResult = "cancelled";
            emailSubject = "Withdrawal Request Cancelled";

            emailMessage = """
        <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden;">
            <div style="background: linear-gradient(135deg, #d32f2f, #f44336); padding: 20px; text-align: center; color: white;">
                <h2 style="margin: 0;">Withdrawal Cancelled</h2>
            </div>
            <div style="padding: 25px; background-color: #fff;">
                <p>Hi <strong>%s</strong>,</p>
                <p>Your withdrawal request has been <strong>CANCELLED</strong>.</p>
                <div style="background-color: #fdecea; border-left: 4px solid #d32f2f; padding: 15px; margin: 20px 0;">
                    <p style="margin: 0;"><strong>Amount:</strong> %s VND</p>
                    <p style="margin: 8px 0 0;"><strong>Bank:</strong> %s - %s - %s</p>
                    <p style="margin: 8px 0 0;"><strong>Cancelled At:</strong> %s</p>
                    <p style="margin: 8px 0 0;"><strong>Reason:</strong> %s</p>
                </div>
                <p>Please review and submit a new request if needed.</p>
            </div>
        </div>
        """.formatted(
                    user.getUsername(),
                    amountVND,
                    bankAccount.getBankName(),
                    bankAccount.getAccountHolderName(),
                    bankAccount.getAccountNumber(),
                    now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    reason
            );
        }


        withdrawalService.save(withdrawalRequest);
        redirectAttributes.addFlashAttribute("success",
                "Withdrawal has been " + actionResult + " successfully.");
        try {
            emailService.sendEmail(new Email(user.getEmail(), emailSubject, emailMessage));
        } catch (Exception e) {
        }

        return "redirect:/admin/withdrawal";
    }
}
