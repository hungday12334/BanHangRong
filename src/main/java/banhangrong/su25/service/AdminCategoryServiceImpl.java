package banhangrong.su25.service;

import banhangrong.su25.DTO.CategoryFilter;
import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Repository.AdminCategoryRepository;
import banhangrong.su25.Repository.CategoriesRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class AdminCategoryServiceImpl implements AdminCategoryService{
    @Autowired
    private AdminCategoryRepository categoriesRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Override
    public List<Categories> findAll() {
        return categoriesRepository.findAll();
    }

    @Override
    public Categories findById(Long id) {
        return categoriesRepository.findById(id).orElse(null);
    }

    @Override
    public Categories save(Categories category) {
        return categoriesRepository.save(category);
    }

    @Override
    public void delete(Long id) {
        categoriesRepository.deleteById(id);
    }

    @Override
    public void update(Categories category) {
        categoriesRepository.save(category);
    }

    @Override
    public List<Categories> filter(CategoryFilter filter) {
        StringBuilder jpql = new StringBuilder("""
        select c from Categories c
        where (:categoryId is null or c.categoryId = :categoryId)
        and (:name is null or lower(c.name) like lower(concat('%', :name, '%')))
        and (:createdFrom is null or c.createdAt >= :createdFrom)
        and (:createdTo is null or c.createdAt <= :createdTo)
        and (:updatedFrom is null or c.updatedAt >= :updatedFrom)
        and (:updatedTo is null or c.updatedAt <= :updatedTo)
    """);

        if (filter.getSortBy() != null && filter.getSortOrder() != null) {
            jpql.append(" ORDER BY c.")
                    .append(filter.getSortBy())
                    .append(" ")
                    .append(filter.getSortOrder());
        }

        Query query = entityManager.createQuery(jpql.toString(), Categories.class);

        query.setParameter("categoryId", filter.getId());
        query.setParameter("name",
                (filter.getName() != null && !filter.getName().isBlank()) ? filter.getName() : null);
        query.setParameter("createdFrom", filter.getCreatedFrom());
        query.setParameter("createdTo", filter.getCreatedTo());
        query.setParameter("updatedFrom", filter.getUpdatedFrom());
        query.setParameter("updatedTo", filter.getUpdatedTo());

        return query.getResultList();
    }

}
