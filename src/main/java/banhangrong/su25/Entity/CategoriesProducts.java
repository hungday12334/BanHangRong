package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "categories_products")

public class CategoriesProducts {
    @EmbeddedId
    private CategoriesProductsId id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CategoriesProductsId getId() {
        return id;
    }

    public void setId(CategoriesProductsId id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

@Embeddable
class CategoriesProductsId implements java.io.Serializable {
    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "product_id")
    private Long productId;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoriesProductsId that = (CategoriesProductsId) o;
        return java.util.Objects.equals(categoryId, that.categoryId) &&
               java.util.Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(categoryId, productId);
    }
}
