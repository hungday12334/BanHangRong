package banhangrong.su25.service;

import banhangrong.su25.DTO.LicenseDTO;
import banhangrong.su25.Entity.CategoryLicenses;
import banhangrong.su25.Entity.ShopLicenses;
import banhangrong.su25.Repository.CategoryLicensesRepository;
import banhangrong.su25.Repository.ShopLicensesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LicenseManagementService {

    @Autowired
    private ShopLicensesRepository shopLicensesRepository;

    @Autowired
    private CategoryLicensesRepository categoryLicensesRepository;

    /**
     * Get all available licenses for a seller
     */
    public List<LicenseDTO> getAllLicensesBySeller(Long sellerId) {
        List<ShopLicenses> licenses = shopLicensesRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        return licenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all licenses for a seller with assignment status for a specific category
     */
    public List<LicenseDTO> getAllLicensesWithAssignmentStatus(Long sellerId, Long categoryId) {
        List<ShopLicenses> allLicenses = shopLicensesRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        List<CategoryLicenses> assignedLicenses = categoryLicensesRepository.findByCategoryIdAndSellerId(categoryId, sellerId);

        List<Long> assignedLicenseIds = assignedLicenses.stream()
                .map(CategoryLicenses::getShopLicenseId)
                .collect(Collectors.toList());

        return allLicenses.stream()
                .map(license -> {
                    LicenseDTO dto = convertToDTO(license);
                    dto.setIsAssignedToCategory(assignedLicenseIds.contains(license.getShopLicenseId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get licenses assigned to a specific category
     */
    public List<LicenseDTO> getAssignedLicensesForCategory(Long categoryId, Long sellerId) {
        List<CategoryLicenses> categoryLicenses = categoryLicensesRepository.findByCategoryIdAndSellerId(categoryId, sellerId);

        List<LicenseDTO> result = new ArrayList<>();
        for (CategoryLicenses cl : categoryLicenses) {
            Optional<ShopLicenses> license = shopLicensesRepository.findById(cl.getShopLicenseId());
            if (license.isPresent()) {
                LicenseDTO dto = convertToDTO(license.get());
                dto.setIsAssignedToCategory(true);
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * Create a new license
     */
    @Transactional
    public ShopLicenses createLicense(ShopLicenses license) {
        return shopLicensesRepository.save(license);
    }

    /**
     * Assign a license to a category
     */
    @Transactional
    public void assignLicenseToCategory(Long categoryId, Long shopLicenseId, Long sellerId) {
        // Check if already assigned
        if (categoryLicensesRepository.existsByCategoryIdAndShopLicenseId(categoryId, shopLicenseId)) {
            throw new RuntimeException("License đã được gán cho danh mục này");
        }

        // Verify the license belongs to the seller
        Optional<ShopLicenses> license = shopLicensesRepository.findById(shopLicenseId);
        if (!license.isPresent()) {
            throw new RuntimeException("License không tồn tại");
        }
        if (!license.get().getSellerId().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền với license này");
        }

        CategoryLicenses categoryLicense = new CategoryLicenses();
        categoryLicense.setCategoryId(categoryId);
        categoryLicense.setShopLicenseId(shopLicenseId);
        categoryLicense.setSellerId(sellerId);

        categoryLicensesRepository.save(categoryLicense);
    }

    /**
     * Remove a license assignment from a category (not delete from database)
     */
    @Transactional
    public void removeLicenseFromCategory(Long categoryId, Long shopLicenseId, Long sellerId) {
        categoryLicensesRepository.deleteByCategoryIdAndShopLicenseIdAndSellerId(categoryId, shopLicenseId, sellerId);
    }

    /**
     * Get count of products in a category
     */
    public long getLicenseCountForCategory(Long categoryId, Long sellerId) {
        return categoryLicensesRepository.countByCategoryIdAndSellerId(categoryId, sellerId);
    }

    /**
     * Convert entity to DTO
     */
    private LicenseDTO convertToDTO(ShopLicenses license) {
        LicenseDTO dto = new LicenseDTO();
        dto.setShopLicenseId(license.getShopLicenseId());
        dto.setLicenseName(license.getLicenseName());
        dto.setLicenseType(license.getLicenseType());
        dto.setLicenseNumber(license.getLicenseNumber());
        dto.setDescription(license.getDescription());
        dto.setStatus(license.getStatus());
        dto.setIsActive(license.getIsActive());
        dto.setIsAssignedToCategory(false);
        return dto;
    }
}

