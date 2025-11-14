package banhangrong.su25.service;

import banhangrong.su25.Entity.WithdrawalRequest;
import banhangrong.su25.Repository.AdminWithdrawalRepository;
import banhangrong.su25.Repository.WithdrawalRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class AdminWithdrawalServiceImpl implements AdminWithdrawalService{

    @Autowired
    private AdminWithdrawalRepository withdrawalRequestRepository;
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

}
