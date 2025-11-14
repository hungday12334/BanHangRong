package banhangrong.su25.service;

import banhangrong.su25.Entity.WithdrawalRequest;

import java.util.List;

public interface AdminWithdrawalService {
    public List<WithdrawalRequest> findAll();
    public WithdrawalRequest findById(Long id);
    public WithdrawalRequest save(WithdrawalRequest withdrawalRequest);
    public void delete(Long id);
    public long countByStatus(String status);
}
