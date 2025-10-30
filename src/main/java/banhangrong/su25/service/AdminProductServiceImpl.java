package banhangrong.su25.service;

import banhangrong.su25.DTO.ProductFilter;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.AdminProductsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminProductServiceImpl implements AdminProductService {
    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    private AdminProductsRepository adminProductsRepository;

    @Override
    public Products save(Products product) {
        return adminProductsRepository.save(product);
    }

    @Override
    public void delete(Long id) {
        adminProductsRepository.deleteById(id);
    }

    @Override
    public void update(Products product) {
        adminProductsRepository.save(product);
    }

    @Override
    public Products findById(Long id) {
        return adminProductsRepository.findById(id).orElse(null);
    }

    @Override
    public List<Products> findAll() {
        return adminProductsRepository.findAll();
    }

    @Override
    public List<Products> findByStatus(String status) {
        // Prefer case-insensitive to match normalized status
        List<Products> list = adminProductsRepository.findByStatusIgnoreCase(status);
        if (list == null || list.isEmpty()) {
            return adminProductsRepository.findByStatus(status);
        }
        return list;
    }

    @Override
    public Long count() {
        return adminProductsRepository.count();
    }

    @Override
    public Long countByStatus(String status) {
        Long c = adminProductsRepository.countByStatusIgnoreCase(status);
        if (c == null || c == 0L) {
            return adminProductsRepository.countByStatus(status);
        }
        return c;
    }

    @Override
    public List<Products> findBySellerIdAndStatusIgnoreCase(Long sellerId, String status) {
        return adminProductsRepository.findBySellerIdAndStatusIgnoreCase(sellerId, status);
    }

    @Override
    public List<Products> filter(ProductFilter filter) {
        // Xử lý các field rỗng → null (để JPQL bỏ qua điều kiện)
        if (filter.getStatus() != null && filter.getStatus().equalsIgnoreCase("")) {
            filter.setStatus(null);
        }

        StringBuilder jpql = new StringBuilder("""
                SELECT p FROM Products p WHERE 
                (:id IS NULL OR p.productId = :id)
                AND (:sellerId IS NULL OR p.sellerId = :sellerId)
                AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
                AND (:minPrice IS NULL OR p.price >= :minPrice)
                AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                AND (:minSalePrice IS NULL OR p.salePrice >= :minSalePrice)
                AND (:maxSalePrice IS NULL OR p.salePrice <= :maxSalePrice)
                AND (:minQuantity IS NULL OR p.quantity >= :minQuantity)
                AND (:maxQuantity IS NULL OR p.quantity <= :maxQuantity)
                AND (:minTotalSales IS NULL OR p.totalSales >= :minTotalSales)
                AND (:maxTotalSales IS NULL OR p.totalSales <= :maxTotalSales)
                AND (:minAvgRating IS NULL OR p.averageRating >= :minAvgRating)
                AND (:maxAvgRating IS NULL OR p.averageRating <= :maxAvgRating)
                AND (:status IS NULL OR LOWER(p.status) = LOWER(:status))
                AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)
                AND (:createdTo IS NULL OR p.createdAt <= :createdTo)
                AND (:updatedFrom IS NULL OR p.updatedAt >= :updatedFrom)
                AND (:updatedTo IS NULL OR p.updatedAt <= :updatedTo)
                """);

        // Thêm ORDER BY nếu có sort
        if (filter.getSortBy() != null && !filter.getSortBy().isBlank()
                && filter.getSortOrder() != null && !filter.getSortOrder().isBlank()) {
            jpql.append(" ORDER BY p.").append(filter.getSortBy()).append(" ").append(filter.getSortOrder());
        }

        Query query = entityManager.createQuery(jpql.toString(), Products.class);

        // Set parameters (chỉ set nếu không null, tránh lỗi)
        query.setParameter("id", filter.getId());
        query.setParameter("sellerId", filter.getSellerId());

        // Text search: chỉ set nếu có giá trị
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.setParameter("name", filter.getName());
        } else {
            query.setParameter("name", null);
        }

        query.setParameter("minPrice", filter.getMinPrice());
        query.setParameter("maxPrice", filter.getMaxPrice());
        query.setParameter("minSalePrice", filter.getMinSalePrice());
        query.setParameter("maxSalePrice", filter.getMaxSalePrice());
        query.setParameter("minQuantity", filter.getMinQuantity());
        query.setParameter("maxQuantity", filter.getMaxQuantity());
        query.setParameter("minTotalSales", filter.getMinTotalSales());
        query.setParameter("maxTotalSales", filter.getMaxTotalSales());
        query.setParameter("minAvgRating", filter.getMinAvgRating());
        query.setParameter("maxAvgRating", filter.getMaxAvgRating());
        query.setParameter("status", filter.getStatus());

        // Date range
        query.setParameter("createdFrom", filter.getCreatedFrom());
        query.setParameter("createdTo", filter.getCreatedTo());
        query.setParameter("updatedFrom", filter.getUpdatedFrom());
        query.setParameter("updatedTo", filter.getUpdatedTo());

        return query.getResultList();
    }

}
