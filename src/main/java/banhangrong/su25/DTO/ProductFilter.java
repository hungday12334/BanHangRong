package banhangrong.su25.DTO;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class ProductFilter {
    private Long id;
    private Long sellerId;
    private String name;
    private String status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minSalePrice;
    private Integer maxSalePrice;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Integer minTotalSales;
    private Integer maxTotalSales;
    private BigDecimal minAvgRating;
    private BigDecimal maxAvgRating;
    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;
    private String sortBy;
    private String sortOrder;
    public ProductFilter() {
    }

    public ProductFilter(Long id, Long sellerId, String name, String status, BigDecimal minPrice, BigDecimal maxPrice, Integer minSalePrice, Integer maxSalePrice, Integer minQuantity, Integer maxQuantity, Integer minTotalSales, Integer maxTotalSales, BigDecimal minAvgRating, BigDecimal maxAvgRating, String createdFrom, String createdTo, String updatedFrom, String updatedTo, String sortBy, String sortOrder) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.status = status;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minSalePrice = minSalePrice;
        this.maxSalePrice = maxSalePrice;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minTotalSales = minTotalSales;
        this.maxTotalSales = maxTotalSales;
        this.minAvgRating = minAvgRating;
        this.maxAvgRating = maxAvgRating;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.updatedFrom = updatedFrom;
        this.updatedTo = updatedTo;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

}
