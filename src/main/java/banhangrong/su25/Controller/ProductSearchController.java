package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.CategoriesRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ProductSearchController {
    private final ProductsRepository productsRepository;
    private final CategoriesRepository categoriesRepository;
    private final ProductImagesRepository productImagesRepository;

    public ProductSearchController(ProductsRepository productsRepository, CategoriesRepository categoriesRepository, ProductImagesRepository productImagesRepository) {
        this.productsRepository = productsRepository;
        this.categoriesRepository = categoriesRepository;
        this.productImagesRepository = productImagesRepository;
    }

    public static class ProductListItem {
        public Long productId;
        public String name;
        public String status;
        public Long sellerId;
        public String categoryName; // first category if any
        public java.math.BigDecimal price;
        public Integer totalSales;
        public java.math.BigDecimal averageRating;
        public String imageUrl;
    }

    private ProductListItem map(Products p, String categoryName) {
        ProductListItem d = new ProductListItem();
        d.productId = p.getProductId();
        d.name = p.getName();
        d.status = p.getStatus();
        d.sellerId = p.getSellerId();
        d.categoryName = categoryName;
        d.price = p.getPrice();
        d.totalSales = p.getTotalSales();
        d.averageRating = p.getAverageRating();
        try {
            var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
            if (primary != null && !primary.isEmpty()) {
                d.imageUrl = primary.get(0).getImageUrl();
            } else {
                var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                if (any != null && !any.isEmpty()) d.imageUrl = any.get(0).getImageUrl();
            }
        } catch (Exception ignored) {}
        return d;
    }

    @GetMapping("/api/products/search")
    public Page<ProductListItem> search(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "18") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "minRating", required = false) Double minRating,
            @RequestParam(name = "minDownloads", required = false) Integer minDownloads
    ) {
        // Start with all products (seller scope can be added later if needed)
    List<Products> all = productsRepository.findAll();
        // Text filter
        if (search != null && !search.trim().isEmpty()) {
            String s = search.trim().toLowerCase();
            all = all.stream().filter(p -> {
                String n = Optional.ofNullable(p.getName()).orElse("").toLowerCase();
                String d = Optional.ofNullable(p.getDescription()).orElse("").toLowerCase();
                return n.contains(s) || d.contains(s);
            }).collect(Collectors.toList());
        }
        // Category filter (best-effort: use repository count mapping check; for simplicity, assume a product belongs to up to one primary category via external mapping not loaded here)
        Map<Long,String> categoryNameByProduct = new HashMap<>();
        if (categoryId != null) {
            try {
                List<Products> inCat = productsRepository.findByCategoryIdAnyStatus(categoryId);
                Set<Long> allowed = inCat.stream().map(Products::getProductId).collect(Collectors.toSet());
                all = all.stream().filter(p -> allowed.contains(p.getProductId())).collect(Collectors.toList());
                categoriesRepository.findById(categoryId).ifPresent(c -> {
                    for (Long pid : allowed) categoryNameByProduct.put(pid, c.getName());
                });
            } catch (Exception ignored) {}
        }
        // Status filter (case-insensitive). Accept comma-separated list of statuses.
        if (status != null && !status.trim().isEmpty()) {
            final Set<String> wanted = Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            if (!wanted.isEmpty()) {
                all = all.stream().filter(p -> {
                    String pst = Optional.ofNullable(p.getStatus()).orElse("").trim().toLowerCase();
                    return wanted.contains(pst);
                }).collect(Collectors.toList());
            }
        }

        // Rating filter
        if (minRating != null) {
            all = all.stream().filter(p -> {
                if (p.getAverageRating() == null) return false;
                try { return p.getAverageRating().doubleValue() >= minRating; } catch (Exception e) { return false; }
            }).collect(Collectors.toList());
        }
        // Downloads filter (use totalSales as downloads/sold)
        if (minDownloads != null) {
            all = all.stream().filter(p -> Optional.ofNullable(p.getTotalSales()).orElse(0) >= minDownloads).collect(Collectors.toList());
        }
        // Sort: newest updated desc then id desc
        all.sort(Comparator.comparing(Products::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(Products::getProductId, Comparator.nullsLast(Comparator.reverseOrder())));

        int from = Math.max(0, page) * Math.max(1, size);
        int to = Math.min(all.size(), from + Math.max(1, size));
        List<Products> slice = from < to ? all.subList(from, to) : Collections.emptyList();

        // Build category name mapping (best-effort: leave blank as we don't have a direct relation entity loaded here)
    Map<Long,String> catName = categoryNameByProduct;
        List<ProductListItem> content = slice.stream()
                .map(p -> map(p, catName.getOrDefault(p.getProductId(), null)))
                .collect(Collectors.toList());
        return new PageImpl<>(content, PageRequest.of(Math.max(0,page), Math.max(1,size)), all.size());
    }

    // Categories for filter dropdown
    @GetMapping("/api/categories")
    public List<Map<String,Object>> categories() {
        List<Categories> list = categoriesRepository.findAllByOrderByNameAsc();
        List<Map<String,Object>> res = new ArrayList<>();
        for (Categories c : list) {
            Map<String,Object> m = new HashMap<>();
            m.put("categoryId", c.getCategoryId());
            m.put("name", c.getName());
            res.add(m);
        }
        return res;
    }
}
