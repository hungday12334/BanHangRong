package banhangrong.su25.DTO;

public class UserFilter {
    private String username;
    private String email;
    private String phoneNumber;
    private String gender;
    private String userType;
    private String active;
    private String verified;
    private String minBalance;
    private String maxBalance;
    private String birthFrom;
    private String birthTo;
    private String lastLoginFrom;
    private String lastLoginTo;
    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;
    private String sortBy;
    private String sortOrder;

    public UserFilter() {
    }

    public UserFilter(String username, String email, String phoneNumber, String gender, String userType, String active, String verified, String minBalance, String maxBalance, String birthFrom, String birthTo, String lastLoginFrom, String lastLoginTo, String createdFrom, String createdTo, String updatedFrom, String updatedTo, String sortBy, String sortOrder) {
        this.username = username;
        this.email = email;
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

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getVerified() {
        return verified;
    }

    public void setVerified(String verified) {
        this.verified = verified;
    }

    public String getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(String minBalance) {
        this.minBalance = minBalance;
    }

    public String getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(String maxBalance) {
        this.maxBalance = maxBalance;
    }

    public String getBirthFrom() {
        return birthFrom;
    }

    public void setBirthFrom(String birthFrom) {
        this.birthFrom = birthFrom;
    }

    public String getBirthTo() {
        return birthTo;
    }

    public void setBirthTo(String birthTo) {
        this.birthTo = birthTo;
    }

    public String getLastLoginFrom() {
        return lastLoginFrom;
    }

    public void setLastLoginFrom(String lastLoginFrom) {
        this.lastLoginFrom = lastLoginFrom;
    }

    public String getLastLoginTo() {
        return lastLoginTo;
    }

    public void setLastLoginTo(String lastLoginTo) {
        this.lastLoginTo = lastLoginTo;
    }

    public String getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(String createdFrom) {
        this.createdFrom = createdFrom;
    }

    public String getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(String createdTo) {
        this.createdTo = createdTo;
    }

    public String getUpdatedFrom() {
        return updatedFrom;
    }

    public void setUpdatedFrom(String updatedFrom) {
        this.updatedFrom = updatedFrom;
    }

    public String getUpdatedTo() {
        return updatedTo;
    }

    public void setUpdatedTo(String updatedTo) {
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
