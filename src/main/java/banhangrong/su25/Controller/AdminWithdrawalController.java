package banhangrong.su25.Controller;

import banhangrong.su25.Entity.BankAccount;
import banhangrong.su25.Entity.WithdrawalRequest;
import banhangrong.su25.service.AdminWithdrawalService;
import banhangrong.su25.service.BankAccountService;
import banhangrong.su25.service.WithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/withdrawal")
public class AdminWithdrawalController {

    @Autowired
    private AdminWithdrawalService withdrawalService;

    @Autowired
    private BankAccountService bankAccountService;

    @GetMapping()
    public String index(Model model) {
        model.addAttribute("withdrawals", withdrawalService.findAll());
        return "admin/withdrawal-management";
    }

    @GetMapping("/detail")
    public String showCreateScreen(Model model, @RequestParam("id") String withdrawalId, RedirectAttributes redirectAttributes) {
        if(withdrawalId == null||withdrawalId.trim().isEmpty()){
            redirectAttributes.addFlashAttribute("error", "Withdrawal not found");
            return "redirect:/admin/withdrawal";
        }
        //Lay withdrawal tu database
        WithdrawalRequest withdrawalRequest = null;
        Long id = null;
        try{
            id = Long.parseLong(withdrawalId);
            withdrawalRequest = withdrawalService.findById(id);
            if(withdrawalRequest == null){
                redirectAttributes.addFlashAttribute("error", "Withdrawal not found");
                return "redirect:/admin/withdrawal";
            }
            model.addAttribute("withdrawalRequest", withdrawalRequest);
        }catch (Exception e){
            redirectAttributes.addFlashAttribute("error", "Invalid withdrawal ID");
            return "redirect:/admin/withdrawal";
        }


        //Lay thong tin ngan hang
        BankAccount bankAccount;
        try{
            bankAccount = bankAccountService.findById(withdrawalRequest.getBankAccountId());
            if(bankAccount == null){
                model.addAttribute("error", "Bank account not found");
            }else{
                model.addAttribute("bankAccount", bankAccount);
            }
        }catch (Exception e){
            model.addAttribute("error", "Invalid bank account ID");
        }
        return "admin/withdrawal-update";
    }
}
