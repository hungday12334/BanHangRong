package banhangrong.su25.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import banhangrong.su25.repository.UsersRepository;
import banhangrong.su25.model.Users;
import java.util.List;

@Controller
public class TestController {

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/")
    public String index(Model model) {
        List<Users> userList = usersRepository.findAll();
        model.addAttribute("userList", userList);
        return "index";
    }
}