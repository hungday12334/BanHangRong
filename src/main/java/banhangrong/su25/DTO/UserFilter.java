package banhangrong.su25.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UserFilter {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String gender;
    private String userType;
    private Boolean active;
    private Boolean verified;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private LocalDate birthFrom;
    private LocalDate birthTo;
    private LocalDate lastLoginFrom;
    private LocalDate lastLoginTo;
    private LocalDate createdFrom;
    private LocalDate createdTo;
    private LocalDate updatedFrom;
    private LocalDate updatedTo;
    private String sortBy;
    private String sortOrder;

    public UserFilter() {
    }

    public UserFilter(Long id, String username, String email, String fullName, String phoneNumber, String gender, String userType, Boolean active, Boolean verified, BigDecimal minBalance, BigDecimal maxBalance, LocalDate birthFrom, LocalDate birthTo, LocalDate lastLoginFrom, LocalDate lastLoginTo, LocalDate createdFrom, LocalDate createdTo, LocalDate updatedFrom, LocalDate updatedTo, String sortBy, String sortOrder) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.userType = userType;
        this.active = active;
        this.verified = verified;
        this.minBalance = minBalance;
        this.maxBalance = maxBalance;
        this.birthFrom = birthFrom;
        this.birthTo = birthTo;
        this.lastLoginFrom = lastLoginFrom;
        this.lastLoginTo = lastLoginTo;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.updatedFrom = updatedFrom;
        this.updatedTo = updatedTo;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(BigDecimal minBalance) {
        this.minBalance = minBalance;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(BigDecimal maxBalance) {
        this.maxBalance = maxBalance;
    }

    public LocalDate getBirthFrom() {
        return birthFrom;
    }

    public void setBirthFrom(LocalDate birthFrom) {
        this.birthFrom = birthFrom;
    }

    public LocalDate getBirthTo() {
        return birthTo;
    }

    public void setBirthTo(LocalDate birthTo) {
        this.birthTo = birthTo;
    }

    public LocalDate getLastLoginFrom() {
        return lastLoginFrom;
    }

    public void setLastLoginFrom(LocalDate lastLoginFrom) {
        this.lastLoginFrom = lastLoginFrom;
    }

    public LocalDate getLastLoginTo() {
        return lastLoginTo;
    }

    public void setLastLoginTo(LocalDate lastLoginTo) {
        this.lastLoginTo = lastLoginTo;
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
