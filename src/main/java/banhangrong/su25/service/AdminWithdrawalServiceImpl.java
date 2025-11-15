package banhangrong.su25.service;

import banhangrong.su25.Entity.WithdrawalRequest;
import banhangrong.su25.Repository.AdminWithdrawalRepository;
import banhangrong.su25.Repository.WithdrawalRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class AdminWithdrawalServiceImpl implements AdminWithdrawalService{

    @Autowired
    private AdminWithdrawalRepository withdrawalRequestRepository;
    @Autowired
    private EntityManager entityManager;
    @Override
    public List<WithdrawalRequest> findAll() {
        return withdrawalRequestRepository.findAll();
    }

    @Override
    public WithdrawalRequest findById(Long id) {
        return withdrawalRequestRepository.findById(id).orElse(null);
    }

    @Override
    public WithdrawalRequest save(WithdrawalRequest withdrawalRequest) {
        return withdrawalRequestRepository.save(withdrawalRequest);
    }

    @Override
    public void delete(Long id) {
        withdrawalRequestRepository.deleteById(id);
    }

    @Override
    public long countByStatus(String status) {
        return withdrawalRequestRepository.countByStatusIgnoreCase(status);
    }

    @Override
    public List<WithdrawalRequest> fiilter(String status, String sortBy, String sortOrder) {
        StringBuilder query = new StringBuilder("SELECT wr FROM WithdrawalRequest wr");
        if (status != null && !status.isEmpty()) {
            query.append(" WHERE lower(wr.status) = lower(:status)");
        }
        if (sortBy != null && sortOrder != null) {
            query.append(" ORDER BY wr.").append(sortBy).append(" ").append(sortOrder);
        }
        Query q = entityManager.createQuery(query.toString(), WithdrawalRequest.class);
        if (status != null && !status.isEmpty()) {
            q.setParameter("status", status);
        }
        return q.getResultList();

    }

}
