package banhangrong.su25.service;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private SessionRegistry sessionRegistry;
    @Override
    public List<Users> findAll() {
        return usersRepository.findAll();
    }

    @Override
    public Users findById(Long id) {
        return usersRepository.findById(id).orElse(null);
    }

    @Override
    public Users save(Users user) {
        return usersRepository.save(user);
    }

    @Override
    public void delete(Long id) {
        usersRepository.deleteById(id);
    }

    @Override
    public void update(Users user) {
        usersRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return usersRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersRepository.existsByEmail(email);
    }

    @Override
    public Long count() {
        return usersRepository.count();
    }

    @Override
    public Long countByUserType(String userType) {
        return usersRepository.countByUserType(userType);
    }

    @Override
    public void setExpireSessionByUsername(String username) {
        for(Object principal : sessionRegistry.getAllPrincipals()){
            if(principal instanceof UserDetails user && user.getUsername().equals(username)){
                for(SessionInformation infor : sessionRegistry.getAllSessions(user, false)){
                    infor.expireNow();
                }
                break;
            }
        }
    }
}
