package banhangrong.su25.service;
import banhangrong.su25.Entity.Products;

import java.util.List;

public interface AdminProductService {
    public Products save(Products product);
    public void delete(Long id);
    public void update(Products product);
    public Products findById(Long id);
    public List<Products> findAll();
    public List<Products> findByStatus(String status);
}
