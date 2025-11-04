package banhangrong.su25.Repository;

import banhangrong.su25.Entity.UserConversationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConversationMetadataRepository extends JpaRepository<UserConversationMetadata, Long> {

    Optional<UserConversationMetadata> findByUserIdAndConversationId(Long userId, String conversationId);

    List<UserConversationMetadata> findByUserId(Long userId);

    List<UserConversationMetadata> findByUserIdAndIsDeletedFalse(Long userId);

    List<UserConversationMetadata> findByUserIdAndIsPinnedTrueAndIsDeletedFalse(Long userId);

    void deleteByUserIdAndConversationId(Long userId, String conversationId);
}

