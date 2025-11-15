package banhangrong.su25.service;

import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Repository.ProductImagesRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ProductImageService {
    private final ProductImagesRepository productImagesRepository;
    public ProductImageService(ProductImagesRepository productImagesRepository) {
        this.productImagesRepository = productImagesRepository;
    }
   public List<ProductImages> getAllProductImages(){
        return productImagesRepository.findAll();
   }
   public ProductImages getProductImagesById(Long id){
        return productImagesRepository.findById(id).orElse(null);
   }
   public ProductImages save(ProductImages productImages){
        return productImagesRepository.save(productImages);
   }
   public void delete(Long id){
        productImagesRepository.deleteById(id);
   }
}
