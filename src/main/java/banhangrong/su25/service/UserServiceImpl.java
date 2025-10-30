package banhangrong.su25.service;

import banhangrong.su25.DTO.UserFilter;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private AdminProductService adminProductService;

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
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails user && user.getUsername().equals(username)) {
                for (SessionInformation infor : sessionRegistry.getAllSessions(user, false)) {
                    infor.expireNow();
                }
                break;
            }
        }
    }

    @Override
    public void deactiveUserById(Users user) {
        setExpireSessionByUsername(user.getUsername());
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getUserType().equalsIgnoreCase("SELLER")) {
            List<Products> listProduct = adminProductService.findBySellerIdAndStatusIgnoreCase(user.getUserId(), "public");
            for (Products p : listProduct) {
                p.setStatus("Cancelled");
                adminProductService.save(p);
            }
        }
        usersRepository.save(user);
    }

    @Override
    public List<Users> filter(UserFilter filter) {
        //Xu ly may cái select
        if(filter.getUserType().equalsIgnoreCase("")){
            filter.setUserType(null);
        }
        if(filter.getGender().equalsIgnoreCase("")){
            filter.setGender(null);
        }
        StringBuilder jpql = new StringBuilder("""
                    SELECT u FROM Users u
                    WHERE (:id IS NULL OR u.userId = :id)
                      AND (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
                      AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                      AND (:fullName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))
                      AND (:phone IS NULL OR u.phoneNumber LIKE CONCAT('%', :phone, '%'))
                      AND (:userType IS NULL OR lower(u.userType) = lower(:userType)))
                      AND (:gender IS NULL OR lower(u.gender) = lower(:gender))
                      AND (:active IS NULL OR u.isActive = :active)
                      AND (:verified IS NULL OR u.isEmailVerified = :verified)
                      AND (:minBalance IS NULL OR u.balance >= :minBalance)
                      AND (:maxBalance IS NULL OR u.balance <= :maxBalance)
                      AND (:birthFrom IS NULL OR u.birthDate >= :birthFrom)
                      AND (:birthTo IS NULL OR u.birthDate <= :birthTo)
                      AND (:lastLoginFrom IS NULL OR u.lastLogin >= :lastLoginFrom)
                      AND (:lastLoginTo IS NULL OR u.lastLogin <= :lastLoginTo)
                      AND (:createdFrom IS NULL OR u.createdAt >= :createdFrom)
                      AND (:createdTo IS NULL OR u.createdAt <= :createdTo)
                      AND (:updatedFrom IS NULL OR u.updatedAt >= :updatedFrom)
                      AND (:updatedTo IS NULL OR u.updatedAt <= :updatedTo)
                """);

        if(filter.getSortBy() != null && filter.getSortOrder() != null){
            jpql.append(" ORDER BY u." + filter.getSortBy() + " " + filter.getSortOrder());
        }
        Query query = entityManager.createQuery(jpql.toString(), Users.class);

        // ✅ Set params
        if (filter.getId() != null)
            query.setParameter("id", filter.getId());
        else query.setParameter("id", null);

        if (filter.getUsername() != null && !filter.getUsername().isBlank())
            query.setParameter("username", filter.getUsername());
        else query.setParameter("username", null);

        if (filter.getEmail() != null && !filter.getEmail().isBlank())
            query.setParameter("email", filter.getEmail());
        else query.setParameter("email", null);

        if(filter.getFullName() != null && !filter.getFullName().isBlank())
            query.setParameter("fullName", filter.getFullName());
        else query.setParameter("fullName", null);

        if (filter.getPhoneNumber() != null && !filter.getPhoneNumber().isBlank())
            query.setParameter("phone", filter.getPhoneNumber());
        else query.setParameter("phone", null);

        query.setParameter("userType", filter.getUserType());
        query.setParameter("gender", filter.getGender());
        query.setParameter("active", filter.getActive());
        query.setParameter("verified", filter.getVerified());
        query.setParameter("minBalance", filter.getMinBalance());
        query.setParameter("maxBalance", filter.getMaxBalance());
        query.setParameter("birthFrom", filter.getBirthFrom());
        query.setParameter("birthTo", filter.getBirthTo());
        query.setParameter("lastLoginFrom", filter.getLastLoginFrom());
        query.setParameter("lastLoginTo", filter.getLastLoginTo());
        query.setParameter("createdFrom", filter.getCreatedFrom());
        query.setParameter("createdTo", filter.getCreatedTo());
        query.setParameter("updatedFrom", filter.getUpdatedFrom());
        query.setParameter("updatedTo", filter.getUpdatedTo());

        return query.getResultList();
    }
}

