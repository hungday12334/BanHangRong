package banhangrong.su25.DTO;

import java.time.LocalDate;

public class CategoryFilter {
    private Long id;
    private String name;
    private LocalDate createdFrom;
    private LocalDate createdTo;
    private LocalDate updatedFrom;
    private LocalDate updatedTo;
    private String sortBy;
    private String sortOrder;
    public CategoryFilter() {
    }
    public CategoryFilter(Long id, String name, LocalDate createdFrom, LocalDate createdTo, LocalDate updatedFrom, LocalDate updatedTo, String sortBy, String sortOrder) {
        this.id = id;
        this.name = name;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.updatedFrom = updatedFrom;
        this.updatedTo = updatedTo;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(LocalDate createdFrom) {
        this.createdFrom = createdFrom;
    }

    public LocalDate getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(LocalDate createdTo) {
        this.createdTo = createdTo;
    }

    public LocalDate getUpdatedFrom() {
        return updatedFrom;
    }

    public void setUpdatedFrom(LocalDate updatedFrom) {
        this.updatedFrom = updatedFrom;
    }

    public LocalDate getUpdatedTo() {
        return updatedTo;
    }

    public void setUpdatedTo(LocalDate updatedTo) {
        this.updatedTo = updatedTo;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
