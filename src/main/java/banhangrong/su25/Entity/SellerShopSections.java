// SellerShopSections.java - CẦN SỬA LẠI
package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seller_shop_sections")
public class SellerShopSections {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false)
    private SectionType sectionType;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "filter_category_id")
    private Long filterCategoryId;

    @Column(name = "filter_price_min")
    private BigDecimal filterPriceMin;

    @Column(name = "filter_price_max")
    private BigDecimal filterPriceMax;

    @Column(name = "max_items")
    private Integer maxItems = 6;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SellerFeaturedProducts> featuredProducts = new ArrayList<>();

    // Constructors
    public SellerShopSections() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SellerShopSections(Long sellerId, SectionType sectionType, String sectionTitle) {
        this();
        this.sellerId = sellerId;
        this.sectionType = sectionType;
        this.sectionTitle = sectionTitle;
    }

    // Getters and Setters
    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public SectionType getSectionType() { return sectionType; }
    public void setSectionType(SectionType sectionType) { this.sectionType = sectionType; }

    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Long getFilterCategoryId() { return filterCategoryId; }
    public void setFilterCategoryId(Long filterCategoryId) { this.filterCategoryId = filterCategoryId; }

    public BigDecimal getFilterPriceMin() { return filterPriceMin; }
    public void setFilterPriceMin(BigDecimal filterPriceMin) { this.filterPriceMin = filterPriceMin; }

    public BigDecimal getFilterPriceMax() { return filterPriceMax; }
    public void setFilterPriceMax(BigDecimal filterPriceMax) { this.filterPriceMax = filterPriceMax; }

    public Integer getMaxItems() { return maxItems; }
    public void setMaxItems(Integer maxItems) { this.maxItems = maxItems; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<SellerFeaturedProducts> getFeaturedProducts() { return featuredProducts; }
    public void setFeaturedProducts(List<SellerFeaturedProducts> featuredProducts) { this.featuredProducts = featuredProducts; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}