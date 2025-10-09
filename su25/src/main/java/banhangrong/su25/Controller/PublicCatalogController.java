package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PublicCatalogController {
    private final ProductsRepository productsRepository;

    public PublicCatalogController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Products> products = productsRepository.findAll(); // could filter by active if needed
        model.addAttribute("products", products);
        return "index"; // templates/index.html
    }
}
