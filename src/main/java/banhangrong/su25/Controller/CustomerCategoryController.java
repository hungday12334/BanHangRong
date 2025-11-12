package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.service.CategoryService;
import banhangrong.su25.service.CategoryViewService;
import banhangrong.su25.service.ProductImageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;

@Controller
public class CustomerCategoryController {
    private final CategoryService categoryService;
    private final CategoryViewService categoryViewService;
    private final ProductImageService productImageService;
    public CustomerCategoryController(CategoryService categoryService, CategoryViewService categoryViewService,ProductImageService productImageService) {
        this.categoryService = categoryService;
        this.categoryViewService = categoryViewService;
        this.productImageService = productImageService;
    }
    @GetMapping("/customer/categories")
    public String category(Model model){
        List<Categories> categoryList = categoryService.getAllCategories();
        Map<Long, Long> numberProductInCate = categoryViewService.countPublicProductsInCategories(categoryList);
        model.addAttribute("categories", categoryList);
        model.addAttribute("numberProductInCate", numberProductInCate);
        return "customer/categories";
    }
    @GetMapping("/category/{categoryId}")
    public String categoryProducts(@PathVariable Long categoryId,
                                   Model model,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String minPrice,
                                   @RequestParam(required = false) String maxPrice,
                                   @RequestParam(defaultValue = "priceAsc") String sort) {
        Categories category = categoryViewService.getCategoryById(categoryId);
        if (category == null) {
            return "redirect:/categories";
        }
        model.addAttribute("category", category);

        List<Products> products = categoryViewService.getAllProductInCate(categoryId);
        if (products == null) {
            products = new ArrayList<>();
        }

        // --- KEYWORD FILTER ---
        String keywordValue = keyword != null ? keyword.trim() : "";
        if (!keywordValue.isEmpty()) {
            String keywordLower = keywordValue.toLowerCase();
            List<Products> keywordFiltered = new ArrayList<>();
            for (Products product : products) {
                if (product != null && product.getName() != null && product.getName().toLowerCase().contains(keywordLower)) {
                    keywordFiltered.add(product);
                }
            }
            products = keywordFiltered;
        }
        model.addAttribute("keyword", keywordValue);

        // --- PRICE FILTERS ---
        BigDecimal minPriceValue = null;
        BigDecimal maxPriceValue = null;

        if (minPrice != null && !minPrice.trim().isEmpty()) {
            try {
                minPriceValue = new BigDecimal(minPrice.trim());
            } catch (NumberFormatException ignored) {
                minPriceValue = null;
            }
        }

        if (maxPrice != null && !maxPrice.trim().isEmpty()) {
            try {
                maxPriceValue = new BigDecimal(maxPrice.trim());
            } catch (NumberFormatException ignored) {
                maxPriceValue = null;
            }
        }

        if (minPriceValue != null) {
            List<Products> minFiltered = new ArrayList<>();
            for (Products product : products) {
                if (product != null && product.getPrice() != null && product.getPrice().compareTo(minPriceValue) >= 0) {
                    minFiltered.add(product);
                }
            }
            products = minFiltered;
        }

        if (maxPriceValue != null) {
            List<Products> maxFiltered = new ArrayList<>();
            for (Products product : products) {
                if (product != null && product.getPrice() != null && product.getPrice().compareTo(maxPriceValue) <= 0) {
                    maxFiltered.add(product);
                }
            }
            products = maxFiltered;
        }

        model.addAttribute("minPrice", minPriceValue != null ? minPriceValue.toPlainString() : "");
        model.addAttribute("maxPrice", maxPriceValue != null ? maxPriceValue.toPlainString() : "");

        // --- SORT ---

        List<Products> productsWithPrice = new ArrayList<>();
        List<Products> productsWithoutPrice = new ArrayList<>();
        
        for (Products product : products) {
            if (product != null && product.getPrice() != null) {
                productsWithPrice.add(product);
            } else {
                productsWithoutPrice.add(product);
            }
        }
        
        //sort asc
        productsWithPrice.sort(Comparator.comparing(Products::getPrice));
        //sort desc
        if ("priceDesc".equalsIgnoreCase(sort)) {
            Collections.reverse(productsWithPrice);
        }

        products = new ArrayList<>();
        products.addAll(productsWithPrice);
        products.addAll(productsWithoutPrice);
        model.addAttribute("sort", sort);

        // --- PREPARE MODEL ---
        model.addAttribute("products", products);

        //get img
        List<ProductImages> productImagesList = productImageService.getAllProductImages();
        //send product images to view
        model.addAttribute("productImages", productImagesList);

        return "customer/category-products";
    }
}

