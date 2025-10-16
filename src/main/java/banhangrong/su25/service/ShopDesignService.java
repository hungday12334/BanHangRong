// ShopDesignService.java - SỬA LẠI HOÀN TOÀN
package banhangrong.su25.service;

import banhangrong.su25.Entity.*;
import banhangrong.su25.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            if (fp.getProduct() != null && fp.getProduct().getIsActive()) {
                products.add(fp.getProduct());
            }
        }
        return products;
    }

    private List<Products> getBestSellerProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findTopBySellerIdOrderByTotalSalesDesc(section.getSellerId());
        return limitProducts(allProducts, section.getMaxItems());
    }

    private List<Products> getTopRatedProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findTopBySellerIdOrderByAverageRatingDesc(section.getSellerId());
        return limitProducts(allProducts, section.getMaxItems());
    }

    private List<Products> getNewArrivalsProducts(SellerShopSections section) {
        List<Products> allProducts = productsRepository.findTopBySellerIdOrderByCreatedAtDesc(section.getSellerId());
        return limitProducts(allProducts, section.getMaxItems());
    }

    private List<Products> getCustomFilteredProducts(SellerShopSections section) {
        List<Products> filteredProducts = productsRepository.findWithCustomFilters(
                section.getSellerId(),
                section.getFilterCategoryId(),
                section.getFilterPriceMin(),
                section.getFilterPriceMax()
        );
        return limitProducts(filteredProducts, section.getMaxItems());
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