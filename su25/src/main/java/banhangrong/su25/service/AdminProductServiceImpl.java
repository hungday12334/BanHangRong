package banhangrong.su25.service;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.AdminProductsRepository;
import banhangrong.su25.Repository.ProductsRepository;
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
        return adminProductsRepository.findByStatus(status);
    }

}
