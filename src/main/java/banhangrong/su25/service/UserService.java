package banhangrong.su25.service;

import banhangrong.su25.DTO.UserFilter;
import banhangrong.su25.Entity.Users;
import org.springframework.security.core.session.SessionRegistry;

import java.util.List;

public interface UserService {
    public List<Users> findAll();
    public Users findById(Long id);
    public Users save(Users user);
    public void delete(Long id);
    public void update(Users user);
    public boolean existsByUsername(String username);
    public boolean existsByEmail(String email);
    public Long count();
    public Long countByUserType(String userType);
    public void setExpireSessionByUsername( String username);
    public void deactiveUserById(Users user);
    public List<Users> filter(UserFilter filter);
}
