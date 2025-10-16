package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UsersApiController {
    private final UsersRepository usersRepository;

    public UsersApiController(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @GetMapping("/api/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam(name = "q", required = false) String q,
                                         @RequestParam(name = "type", required = false) String type,
                                         @RequestParam(name = "page", defaultValue = "0") int page,
                                         @RequestParam(name = "size", defaultValue = "10") int size) {
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        Page<Users> p = usersRepository.searchUsers((type != null && !type.isBlank()) ? type.trim() : null,
                (q != null && !q.isBlank()) ? q.trim() : null,
                pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("content", p.getContent());
        body.put("page", p.getNumber());
        body.put("size", p.getSize());
        body.put("totalPages", p.getTotalPages());
        body.put("totalElements", p.getTotalElements());
        return ResponseEntity.ok(body);
    }
}
