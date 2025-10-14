package banhangrong.su25.service;

import banhangrong.su25.Entity.*;
import banhangrong.su25.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShopDesignService {

    private final SellerShopSectionsRepository sectionsRepository;
    private final SellerFeaturedProductsRepository featuredProductsRepository;
    private final ProductsRepository productsRepository;

    @Autowired
    public ShopDesignService(
            SellerShopSectionsRepository sectionsRepository,
            SellerFeaturedProductsRepository featuredProductsRepository,
            ProductsRepository productsRepository) {
        this.sectionsRepository = sectionsRepository;
        this.featuredProductsRepository = featuredProductsRepository;
        this.productsRepository = productsRepository;
    }

    // Lấy cấu hình cửa hàng
    public List<SellerShopSections> getShopSections(Long sellerId) {
        return sectionsRepository.findBySellerIdAndIsActiveTrueOrderBySortOrderAsc(sellerId);
    }

    // Lưu cấu hình cửa hàng
    @Transactional
    public void saveShopLayout(Long sellerId, List<SellerShopSections> sections) {
        // Xóa cấu hình cũ
        sectionsRepository.deleteBySellerId(sellerId);

        // Lưu cấu hình mới
        for (int i = 0; i < sections.size(); i++) {
            SellerShopSections section = sections.get(i);
            section.setSellerId(sellerId);
            section.setSortOrder(i);
            sectionsRepository.save(section);
        }
    }

    // Lấy sản phẩm theo section type để hiển thị
    public List<Products> getProductsForSection(SellerShopSections section) {
        // Tạo Pageable với limit
        Integer maxItems = section.getMaxItems() != null ? section.getMaxItems() : 10;
        Pageable pageable = PageRequest.of(0, maxItems);

        switch (section.getSectionType()) {
            case FEATURED:
                return getFeaturedProducts(section);
            case BEST_SELLER:
                return productsRepository.findTopBestSellersBySellerId(section.getSellerId(), pageable);
            case TOP_RATED:
                return productsRepository.findTopRatedBySellerId(section.getSellerId(), pageable);
            case NEW_ARRIVALS:
                return productsRepository.findNewArrivalsBySellerId(section.getSellerId(), pageable);
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
            products.add(fp.getProduct());
        }
        return products;
    }

    private List<Products> getCustomFilteredProducts(SellerShopSections section) {
        return productsRepository.findWithCustomFilters(
                section.getSellerId(),
                section.getFilterCategoryId(),
                section.getFilterPriceMin(),
                section.getFilterPriceMax(),
                section.getMaxItems() != null ? section.getMaxItems() : 10
        );
    }
}