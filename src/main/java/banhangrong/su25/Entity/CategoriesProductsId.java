package banhangrong.su25.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CategoriesProductsId implements Serializable {
    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "product_id")
    private Long productId;

    public CategoriesProductsId() {}

    public CategoriesProductsId(Long categoryId, Long productId) {
        this.categoryId = categoryId;
        this.productId = productId;
    }

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
        return Objects.equals(categoryId, that.categoryId) &&
               Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, productId);
    }
}
