package banhangrong.su25.DTO;

public class LicenseDTO {
    private Long shopLicenseId;
    private String licenseName;
    private String licenseType;
    private String licenseNumber;
    private String description;
    private String status;
    private Boolean isActive;
    private Boolean isAssignedToCategory; // New field to track if assigned to current category

    // Constructors
    public LicenseDTO() {}

    public LicenseDTO(Long shopLicenseId, String licenseName, String licenseType,
                     String licenseNumber, String description, String status, Boolean isActive) {
        this.shopLicenseId = shopLicenseId;
        this.licenseName = licenseName;
        this.licenseType = licenseType;
        this.licenseNumber = licenseNumber;
        this.description = description;
        this.status = status;
        this.isActive = isActive;
        this.isAssignedToCategory = false;
    }

    // Getters and Setters
    public Long getShopLicenseId() {
        return shopLicenseId;
    }

    public void setShopLicenseId(Long shopLicenseId) {
        this.shopLicenseId = shopLicenseId;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsAssignedToCategory() {
        return isAssignedToCategory;
    }

    public void setIsAssignedToCategory(Boolean isAssignedToCategory) {
        this.isAssignedToCategory = isAssignedToCategory;
    }
}

