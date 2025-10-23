package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ChatRoom;
import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.seller = :seller AND cr.customer = :customer AND cr.product.productId = :productId")
    Optional<ChatRoom> findBySellerAndCustomerAndProduct(@Param("seller") Users seller,
                                                         @Param("customer") Users customer,
                                                         @Param("productId") Long productId);

    List<ChatRoom> findBySeller(Users seller);

    List<ChatRoom> findByCustomer(Users customer);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.seller = :user OR cr.customer = :user ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUser(@Param("user") Users user);

    Optional<ChatRoom> findBySellerAndCustomer(Users seller, Users customer);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.seller = :seller AND cr.customer = :customer AND cr.product IS NULL")
    Optional<ChatRoom> findBySellerAndCustomerNoProduct(@Param("seller") Users seller,
                                                        @Param("customer") Users customer);
}