package banhangrong.su25.service;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductsRepository productsRepository;

    public ProductService(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }
    public List<Products> getAllProducts(){
        return productsRepository.findAll();
    }
    public Products getProductById(Long id){
        return productsRepository.findById(id).orElse(null);
    }
    public Products save(Products product){
        return productsRepository.save(product);
    }

    public void delete(Long id){}
    public List<Products> getAllProductByCategory(Long id){
        return productsRepository.findByCategoryId(id);
    }
}
