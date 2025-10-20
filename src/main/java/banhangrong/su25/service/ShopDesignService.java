// ShopDesignService.java - SỬA LẠI HOÀN TOÀN
package banhangrong.su25.service;

import banhangrong.su25.Entity.*;
import banhangrong.su25.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ShopDesignService {

    private final SellerShopSectionsRepository sectionsRepository;
    private final SellerFeaturedProductsRepository featuredProductsRepository;
    private final ProductsRepository productsRepository;

    // Constructor injection
    public ShopDesignService(
            SellerShopSectionsRepository sectionsRepository,
            SellerFeaturedProductsRepository featuredProductsRepository,
            ProductsRepository productsRepository) {
        this.sectionsRepository = sectionsRepository;
        this.featuredProductsRepository = featuredProductsRepository;
        this.productsRepository = productsRepository;
    }

    // Lấy cấu hình cửa hàng - FIXED
    public List<SellerShopSections> getShopSections(Long sellerId) {
        return sectionsRepository.findBySellerIdAndIsActiveTrueOrderBySortOrderAsc(sellerId);
    }

    // Lưu cấu hình cửa hàng - FIXED
    @Transactional
    public void saveShopLayout(Long sellerId, List<SellerShopSections> sections) {
        // Xóa cấu hình cũ
        sectionsRepository.deleteBySellerId(sellerId);

        // Lưu cấu hình mới
        for (int i = 0; i < sections.size(); i++) {
            SellerShopSections section = sections.get(i);
            section.setSellerId(sellerId);
            section.setSortOrder(i);
            section.setIsActive(true);

            SellerShopSections savedSection = sectionsRepository.save(section);

            // Xử lý featured products nếu là section FEATURED
            if (section.getSectionType() == SectionType.FEATURED &&
                    section.getFeaturedProducts() != null &&
                    !section.getFeaturedProducts().isEmpty()) {

                saveFeaturedProducts(savedSection, section.getFeaturedProducts());
            }
        }
    }

    // Lưu featured products - METHOD MỚI
    @Transactional
    public void saveFeaturedProducts(SellerShopSections section, List<SellerFeaturedProducts> featuredProducts) {
        // Xóa featured products cũ
        featuredProductsRepository.deleteBySectionSectionId(section.getSectionId());

        // Lưu featured products mới
        for (int i = 0; i < featuredProducts.size(); i++) {
            SellerFeaturedProducts featuredProduct = featuredProducts.get(i);
            featuredProduct.setSection(section);
            featuredProduct.setSortOrder(i);
            featuredProductsRepository.save(featuredProduct);
        }
    }

    // Lấy sản phẩm theo section type để hiển thị - FIXED
    public List<Products> getProductsForSection(SellerShopSections section) {
        switch (section.getSectionType()) {
            case FEATURED:
                return getFeaturedProducts(section);
            case BEST_SELLER:
                return getBestSellerProducts(section);
            case TOP_RATED:
                return getTopRatedProducts(section);
            case NEW_ARRIVALS:
                return getNewArrivalsProducts(section);
            case CUSTOM:
                return getCustomFilteredProducts(section);
            default:
                return new ArrayList<>();
        }
    }

    private List<Products> getFeaturedProducts(SellerShopSections section) {
        List<SellerFeaturedProducts> featured = featuredProductsRepository
                .findBySectionSectionIdOrderBySortOrderAsc(section.getSectionId());

        List<Products> products = new ArrayList<>();
        for (SellerFeaturedProducts fp : featured) {
            if (fp.getProduct() != null && "public".equals(fp.getProduct().getStatus())) {
                products.add(fp.getProduct());
            }
        }
        return products;
    }

    private List<Products> getBestSellerProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findBySellerId(section.getSellerId());
        // Sort by total sales descending
        allProducts.sort((a, b) -> {
            Integer salesA = a.getTotalSales() != null ? a.getTotalSales() : 0;
            Integer salesB = b.getTotalSales() != null ? b.getTotalSales() : 0;
            return salesB.compareTo(salesA);
        });
        return limitProducts(allProducts, section.getMaxItems());
    }

    private List<Products> getTopRatedProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findBySellerId(section.getSellerId());
        // Sort by average rating descending
        allProducts.sort((a, b) -> {
            BigDecimal ratingA = a.getAverageRating() != null ? a.getAverageRating() : BigDecimal.ZERO;
            BigDecimal ratingB = b.getAverageRating() != null ? b.getAverageRating() : BigDecimal.ZERO;
            return ratingB.compareTo(ratingA);
        });
        return limitProducts(allProducts, section.getMaxItems());
    }

    private List<Products> getNewArrivalsProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findBySellerId(section.getSellerId());
        // Sort by created date descending
        allProducts.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return limitProducts(allProducts, section.getMaxItems());
    }

    private List<Products> getCustomFilteredProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findBySellerId(section.getSellerId());
        // Apply custom filters manually (simplified version)
        return allProducts.stream()
                .filter(product -> section.getFilterPriceMin() == null || 
                        product.getPrice() != null && 
                        product.getPrice().compareTo(section.getFilterPriceMin()) >= 0)
                .filter(product -> section.getFilterPriceMax() == null || 
                        product.getPrice() != null && 
                        product.getPrice().compareTo(section.getFilterPriceMax()) <= 0)
                .limit(section.getMaxItems() != null ? section.getMaxItems() : 6)
                .toList();
    }

    private List<Products> limitProducts(List<Products> products, Integer maxItems) {
        if (maxItems == null || maxItems <= 0) {
            maxItems = 6; // Default value
        }
        return products.stream()
                .limit(maxItems)
                .toList();
    }

    // Thêm method để quản lý featured products - METHOD MỚI
    @Transactional
    public void updateFeaturedProducts(Long sectionId, List<Long> productIds) {
        Optional<SellerShopSections> sectionOpt = sectionsRepository.findById(sectionId);
        if (sectionOpt.isEmpty()) {
            throw new RuntimeException("Section not found");
        }

        SellerShopSections section = sectionOpt.get();

        // Xóa featured products cũ
        featuredProductsRepository.deleteBySectionSectionId(sectionId);

        // Thêm featured products mới
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Optional<Products> productOpt = productsRepository.findById(productId);

            if (productOpt.isPresent() && productOpt.get().getSellerId().equals(section.getSellerId())) {
                SellerFeaturedProducts featuredProduct = new SellerFeaturedProducts();
                featuredProduct.setSection(section);
                featuredProduct.setProduct(productOpt.get());
                featuredProduct.setSortOrder(i);
                featuredProductsRepository.save(featuredProduct);
            }
        }
    }
}
