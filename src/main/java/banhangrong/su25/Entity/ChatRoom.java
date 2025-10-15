package banhangrong.su25.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Users seller;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Users customer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Products product;

    private Boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors, getters, setters
    public ChatRoom() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Users getSeller() { return seller; }
    public void setSeller(Users seller) { this.seller = seller; }

    public Users getCustomer() { return customer; }
    public void setCustomer(Users customer) { this.customer = customer; }

    public Products getProduct() { return product; }
    public void setProduct(Products product) { this.product = product; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}