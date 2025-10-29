package banhangrong.su25.Repository;


import banhangrong.su25.Entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByCustomerId(Long customerId);

    List<Conversation> findBySellerId(Long sellerId);

    Optional<Conversation> findByCustomerIdAndSellerId(Long customerId, Long sellerId);

    @Query("SELECT c FROM Conversation c WHERE (c.customerId = :userId OR c.sellerId = :userId) ORDER BY c.lastMessageTime DESC NULLS LAST")
    List<Conversation> findConversationsByUserId(@Param("userId") Long userId);
}

