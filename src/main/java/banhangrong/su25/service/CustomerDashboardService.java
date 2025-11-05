package banhangrong.su25.service;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class xử lý business logic cho Customer Dashboard
 * Controller sẽ gọi Service này thay vì gọi trực tiếp Repository
 */
@Service
public class CustomerDashboardService {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CustomerDashboardService(
            ProductsRepository productsRepository,
            ProductImagesRepository productImagesRepository,
            ShoppingCartRepository shoppingCartRepository,
            UsersRepository usersRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * Lấy danh sách sản phẩm công khai (Public) với phân trang
     * Sắp xếp theo: totalSales giảm dần, sau đó createdAt giảm dần
     * 
     * @param page số trang (bắt đầu từ 0)
     * @param size số sản phẩm mỗi trang
     * @param search từ khóa tìm kiếm (có thể null)
     * @return Page chứa danh sách sản phẩm
     */
    public Page<Products> getPublicProducts(int page, int size, String search) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        // Tạo PageRequest với sắp xếp
        // Sort.by: sắp xếp theo nhiều trường
        // Sort.Order.desc: sắp xếp giảm dần
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(
                        Sort.Order.desc("totalSales"),      // Sắp xếp theo số lượng bán giảm dần
                        Sort.Order.desc("createdAt")        // Sau đó sắp xếp theo ngày tạo giảm dần
                ));

        // Nếu có từ khóa tìm kiếm
        if (search != null && !search.trim().isEmpty()) {
            // Tìm theo tên hoặc mô tả (không phân biệt hoa thường)
            String searchTerm = search.trim();
            return productsRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(
                    searchTerm, searchTerm, "Public", pageable);
        } else {
            // Không có tìm kiếm: lấy tất cả sản phẩm Public
            return productsRepository.findByStatus("Public", pageable);
        }
    }

    /**
     * Lấy hình ảnh chính cho danh sách sản phẩm
     * Với mỗi sản phẩm, tìm hình ảnh primary, nếu không có thì lấy hình đầu tiên
     * 
     * @param products danh sách sản phẩm
     * @return Map với key = productId, value = imageUrl
     */
    public Map<Long, String> getProductImages(List<Products> products) {
        // Tạo Map để lưu productId -> imageUrl
        Map<Long, String> imageMap = new HashMap<>();

        // Duyệt từng sản phẩm
        for (Products product : products) {
            Long productId = product.getProductId();
            String imageUrl = null;

            try {
                // Bước 1: Tìm hình ảnh primary (isPrimary = true)
                var primaryImages = productImagesRepository
                        .findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(productId);

                // Nếu có hình primary
                if (primaryImages != null && !primaryImages.isEmpty()) {
                    imageUrl = primaryImages.get(0).getImageUrl();
                } else {
                    // Bước 2: Nếu không có primary, lấy hình đầu tiên
                    var anyImages = productImagesRepository
                            .findTop1ByProductIdOrderByImageIdAsc(productId);

                    if (anyImages != null && !anyImages.isEmpty()) {
                        imageUrl = anyImages.get(0).getImageUrl();
                    }
                }
            } catch (Exception e) {
                // Nếu có lỗi, bỏ qua (không ảnh hưởng đến các sản phẩm khác)
            }

            // Nếu có hình ảnh và không rỗng, thêm vào Map
            if (imageUrl != null && !imageUrl.isBlank()) {
                imageMap.put(productId, imageUrl);
            }
        }

        return imageMap;
    }

    /**
     * Đếm số lượng sản phẩm trong giỏ hàng của user
     * 
     * @param userId ID của user
     * @return số lượng sản phẩm trong giỏ hàng
     */
    public Long getCartCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return shoppingCartRepository.countByUserId(userId);
    }

    /**
     * Lấy thông tin user hiện tại từ SecurityContext
     * 
     * @return Users object nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    public Users getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        String username = auth.getName();
        return usersRepository.findByUsername(username).orElse(null);
    }

    public Users getUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return usersRepository.findByUsername(username).orElse(null);
    }

    /**
     * Kiểm tra xem user có phải CUSTOMER và đã verify email chưa
     * 
     * @param user user cần kiểm tra
     * @return true nếu là CUSTOMER và đã verify email
     */
    public boolean isCustomerEmailVerified(Users user) {
        if (user == null) {
            return false;
        }
        if (!"CUSTOMER".equals(user.getUserType())) {
            return true; // Nếu không phải CUSTOMER thì không cần check email
        }
        return Boolean.TRUE.equals(user.getIsEmailVerified());
    }

    public Page<Products> getFilteredProducts(int page, int size, String search, Long categoryId, 
                                               BigDecimal minPrice, BigDecimal maxPrice, 
                                               BigDecimal minRating, String sortBy) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        StringBuilder jpql = new StringBuilder("SELECT p FROM Products p WHERE LOWER(p.status) = 'public'");
        
        if (search != null && !search.trim().isEmpty()) {
            jpql.append(" AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))");
        }
        
        if (categoryId != null) {
            jpql.append(" AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)");
        }
        
        if (minPrice != null) {
            jpql.append(" AND (p.salePrice IS NOT NULL AND p.salePrice >= :minPrice OR p.salePrice IS NULL AND p.price >= :minPrice)");
        }
        
        if (maxPrice != null) {
            jpql.append(" AND (p.salePrice IS NOT NULL AND p.salePrice <= :maxPrice OR p.salePrice IS NULL AND p.price <= :maxPrice)");
        }
        
        if (minRating != null) {
            jpql.append(" AND (p.averageRating IS NOT NULL AND p.averageRating >= :minRating)");
        }

        if (sortBy != null && !sortBy.trim().isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "price_asc":
                    jpql.append(" ORDER BY COALESCE(p.salePrice, p.price) ASC");
                    break;
                case "price_desc":
                    jpql.append(" ORDER BY COALESCE(p.salePrice, p.price) DESC");
                    break;
                case "rating":
                    jpql.append(" ORDER BY p.averageRating DESC NULLS LAST");
                    break;
                case "newest":
                    jpql.append(" ORDER BY p.createdAt DESC");
                    break;
                case "sales":
                    jpql.append(" ORDER BY p.totalSales DESC");
                    break;
                default:
                    jpql.append(" ORDER BY p.totalSales DESC, p.createdAt DESC");
            }
        } else {
            jpql.append(" ORDER BY p.totalSales DESC, p.createdAt DESC");
        }

        Query query = entityManager.createQuery(jpql.toString(), Products.class);
        
        if (search != null && !search.trim().isEmpty()) {
            query.setParameter("search", search.trim());
        }
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        if (minRating != null) {
            query.setParameter("minRating", minRating);
        }

        @SuppressWarnings("unchecked")
        List<Products> allProducts = (List<Products>) query.getResultList();
        int total = allProducts.size();
        
        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, total);
        List<Products> pageProducts = start < total ? allProducts.subList(start, end) : new java.util.ArrayList<>();

        return new PageImpl<>(pageProducts, PageRequest.of(safePage, safeSize), total);
    }
}

