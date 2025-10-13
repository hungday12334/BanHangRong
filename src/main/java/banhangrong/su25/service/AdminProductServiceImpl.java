package banhangrong.su25.service;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.AdminProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class AdminProductServiceImpl implements AdminProductService {
    @Autowired
    private AdminProductsRepository adminProductsRepository;
    @Override
    public Products save(Products product) {
        return adminProductsRepository.save(product);
    }

    @Override
    public void delete(Long id) {
        adminProductsRepository.deleteById(id);
    }

    @Override
    public void update(Products product) {
        adminProductsRepository.save(product);
    }

    @Override
    public Products findById(Long id) {
        return adminProductsRepository.findById(id).orElse(null);
    }

    @Override
    public List<Products> findAll() {
        return adminProductsRepository.findAll();
    }

    @Override
    public List<Products> findByStatus(String status) {
        // Prefer case-insensitive to match normalized status
        List<Products> list = adminProductsRepository.findByStatusIgnoreCase(status);
        if (list == null || list.isEmpty()) {
            return adminProductsRepository.findByStatus(status);
        }
        return list;
    }

    @Override
    public Long count() {
        return adminProductsRepository.count();
    }

    @Override
    public Long countByStatus(String status) {
        Long c = adminProductsRepository.countByStatusIgnoreCase(status);
        if (c == null || c == 0L) {
            return adminProductsRepository.countByStatus(status);
        }
        return c;
    }

}
