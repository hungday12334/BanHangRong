package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TestController {

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/test")
    public String index(Model model) {
        List<Users> userList = usersRepository.findAll();
        model.addAttribute("userList", userList);
        return "index";
    }
}