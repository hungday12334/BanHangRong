package banhangrong.su25.Config;

import banhangrong.su25.service.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private AdminProductService adminProductService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        long pendingCount = adminProductService.countByStatus("pending");
        model.addAttribute("pendingCount", pendingCount);
    }
}
