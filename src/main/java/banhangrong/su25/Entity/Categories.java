package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "categories")
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    private String name;
    private String description;

    // ===== NEW FIELDS FOR ENHANCED CATEGORY MANAGEMENT =====

    @Column(name = "slug", length = 100)
    private String slug;  // URL-friendly name (e.g., "electronics-technology")

    @Column(name = "parent_id")
    private Long parentId;  // For subcategories (null = root category)

    @Column(name = "icon", length = 50)
    private String icon;  // Icon class name (e.g., "ti-device-laptop")

    @Column(name = "image_url", length = 255)
    private String imageUrl;  // Category image URL

    @Column(name = "sort_order")
    private Integer sortOrder = 0;  // Display order (lower = first)

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";  // ACTIVE, HIDDEN, DRAFT

    @Column(name = "featured")
    private Boolean featured = false;  // Featured on homepage

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CategoriesProducts> categoryProducts;

    // Getters & Setters
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ===== NEW GETTERS & SETTERS =====

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<CategoriesProducts> getCategoryProducts() { return categoryProducts; }
    public void setCategoryProducts(List<CategoriesProducts> categoryProducts) { this.categoryProducts = categoryProducts; }
}
