package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    @Autowired
    private ProductsRepository productsRepository;

    // Assume you have a way to get the current seller's ID
    private Long getCurrentSellerId(Authentication authentication) {
        // Placeholder: Implement your actual logic to get the seller ID from the authentication principal
        // For example: CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        // return userDetails.getId();
        return 1L; // Replace with actual seller ID
    }

    @GetMapping
    public ResponseEntity<List<Products>> getSellerProducts(Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);
        // We only need active/public products for selection
        List<Products> products = productsRepository.findBySellerIdAndStatus(sellerId, "public");
        return ResponseEntity.ok(products);
    }
}
