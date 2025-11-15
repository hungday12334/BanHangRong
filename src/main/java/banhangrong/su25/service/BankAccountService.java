package banhangrong.su25.service;

import banhangrong.su25.Entity.BankAccount;

public interface BankAccountService {
    public BankAccount findById(Long id);
    public BankAccount save(BankAccount bankAccount);
    public void delete(Long id);
}
