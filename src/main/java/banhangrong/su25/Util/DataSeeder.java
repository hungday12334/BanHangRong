package banhangrong.su25.Util;

import banhangrong.su25.Entity.*;
import banhangrong.su25.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private CategoriesRepository categoriesRepository;
    
    @Autowired
    private ProductsRepository productsRepository;
    
    @Autowired
    private CategoriesProductsRepository categoriesProductsRepository;
    
    @Autowired
    private ProductImagesRepository productImagesRepository;
    
    @Autowired
    private ProductReviewsRepository productReviewsRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Chỉ chạy nếu chưa có dữ liệu
        if (productsRepository.count() > 0) {
            System.out.println("Database đã có dữ liệu, bỏ qua seeding...");
            return;
        }

        System.out.println("Bắt đầu thêm dữ liệu mẫu...");

        // Tạo users (sellers và customers)
        Users seller1 = createUser(1L, "seller1", "Nguyễn Văn A", "seller1@example.com", "SELLER");
        Users seller2 = createUser(2L, "seller2", "Trần Thị B", "seller2@example.com", "SELLER");
        Users customer1 = createUser(3L, "customer1", "Lê Văn C", "customer1@example.com", "CUSTOMER");

        // Tạo categories
        Categories cat1 = createCategory(1L, "Phần mềm", "Các phần mềm ứng dụng");
        Categories cat2 = createCategory(2L, "Game", "Trò chơi điện tử");
        Categories cat3 = createCategory(3L, "Template", "Mẫu thiết kế");
        Categories cat4 = createCategory(4L, "Ebook", "Sách điện tử");
        createCategory(5L, "Khóa học", "Khóa học online");

        // Tạo products
        Products product1 = createProduct(1L, seller1.getUserId(), "Adobe Photoshop 2024", 
            "Phần mềm chỉnh sửa ảnh chuyên nghiệp với đầy đủ tính năng mới nhất", 
            new BigDecimal("2500000.00"), new BigDecimal("2000000.00"), 50, "Public");
        
        Products product2 = createProduct(2L, seller1.getUserId(), "Microsoft Office 2024", 
            "Bộ ứng dụng văn phòng đầy đủ Word, Excel, PowerPoint", 
            new BigDecimal("1800000.00"), null, 100, "Public");
        
        Products product3 = createProduct(3L, seller2.getUserId(), "Game Minecraft Premium", 
            "Trò chơi xây dựng thế giới 3D sáng tạo", 
            new BigDecimal("800000.00"), new BigDecimal("600000.00"), 200, "Public");
        
        Products product4 = createProduct(4L, seller2.getUserId(), "Template Website Responsive", 
            "Mẫu website responsive đẹp mắt cho doanh nghiệp", 
            new BigDecimal("500000.00"), null, 30, "Public");
        
        Products product5 = createProduct(5L, seller1.getUserId(), "Ebook Lập trình Java", 
            "Sách điện tử hướng dẫn lập trình Java từ cơ bản đến nâng cao", 
            new BigDecimal("200000.00"), new BigDecimal("150000.00"), 1000, "Public");

        // Liên kết sản phẩm với danh mục
        createCategoryProduct(cat1, product1);
        createCategoryProduct(cat1, product2);
        createCategoryProduct(cat2, product3);
        createCategoryProduct(cat3, product4);
        createCategoryProduct(cat4, product5);

        // Thêm hình ảnh sản phẩm
        createProductImage(product1.getProductId(), "/img/avatar_default.jpg", true);
        createProductImage(product2.getProductId(), "/img/avatar_default.jpg", true);
        createProductImage(product3.getProductId(), "/img/avatar_default.jpg", true);
        createProductImage(product4.getProductId(), "/img/avatar_default.jpg", true);
        createProductImage(product5.getProductId(), "/img/avatar_default.jpg", true);

        // Thêm đánh giá mẫu
        createProductReview(product1.getProductId(), customer1.getUserId(), 5, "Phần mềm rất tốt, đầy đủ tính năng");
        createProductReview(product2.getProductId(), customer1.getUserId(), 4, "Office 2024 có nhiều cải tiến mới");
        createProductReview(product3.getProductId(), customer1.getUserId(), 5, "Game hay, đồ họa đẹp");

        System.out.println("Đã thêm thành công " + productsRepository.count() + " sản phẩm!");
    }

    private Users createUser(Long userId, String username, String fullName, String email, String userType) {
        // Kiểm tra xem user đã tồn tại chưa
        if (usersRepository.existsById(userId)) {
            return usersRepository.findById(userId).orElse(null);
        }
        
        Users user = new Users();
        // Không set ID để Hibernate tự generate
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"); // password: 123456
        user.setUserType(userType);
        user.setIsEmailVerified(true);
        user.setIsActive(true);
        user.setBalance(new BigDecimal("1000000.00"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return usersRepository.save(user);
    }

    private Categories createCategory(Long categoryId, String name, String description) {
        // Kiểm tra xem category đã tồn tại chưa
        if (categoriesRepository.existsById(categoryId)) {
            return categoriesRepository.findById(categoryId).orElse(null);
        }
        
        Categories category = new Categories();
        // Không set ID để Hibernate tự generate
        category.setName(name);
        category.setDescription(description);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return categoriesRepository.save(category);
    }

    private Products createProduct(Long productId, Long sellerId, String name, String description, 
                                 BigDecimal price, BigDecimal salePrice, Integer quantity, String status) {
        // Kiểm tra xem product đã tồn tại chưa
        if (productsRepository.existsById(productId)) {
            return productsRepository.findById(productId).orElse(null);
        }
        
        Products product = new Products();
        // Không set ID để Hibernate tự generate
        product.setSellerId(sellerId);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setSalePrice(salePrice);
        product.setQuantity(quantity);
        product.setDownloadUrl("https://example.com/download/" + name.toLowerCase().replace(" ", "_") + ".zip");
        product.setTotalSales(0);
        product.setAverageRating(new BigDecimal("4.5"));
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productsRepository.save(product);
    }

    private void createCategoryProduct(Categories category, Products product) {
        CategoriesProductsId id = new CategoriesProductsId(category.getCategoryId(), product.getProductId());
        
        CategoriesProducts cp = new CategoriesProducts();
        cp.setId(id);
        cp.setCreatedAt(LocalDateTime.now());
        categoriesProductsRepository.save(cp);
    }

    private void createProductImage(Long productId, String imageUrl, Boolean isPrimary) {
        ProductImages image = new ProductImages();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setIsPrimary(isPrimary);
        image.setCreatedAt(LocalDateTime.now());
        productImagesRepository.save(image);
    }

    private void createProductReview(Long productId, Long userId, Integer rating, String comment) {
        ProductReviews review = new ProductReviews();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        productReviewsRepository.save(review);
    }
}
