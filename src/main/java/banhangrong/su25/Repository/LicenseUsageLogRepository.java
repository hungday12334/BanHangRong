package banhangrong.su25.Repository;

import banhangrong.su25.Entity.LicenseUsageLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LicenseUsageLogRepository extends JpaRepository<LicenseUsageLog, Long> {
    @Query("SELECT l FROM LicenseUsageLog l WHERE l.licenseId = :licenseId ORDER BY l.eventTime DESC")
    List<LicenseUsageLog> findByLicenseIdOrderByEventTimeDesc(@Param("licenseId") Long licenseId, Pageable pageable);

    // Pageable-aware method returning a Page for easier pagination metadata
    @Query("SELECT l FROM LicenseUsageLog l WHERE l.licenseId = :licenseId ORDER BY l.eventTime DESC")
    org.springframework.data.domain.Page<LicenseUsageLog> findPageByLicenseIdOrderByEventTimeDesc(@Param("licenseId") Long licenseId, Pageable pageable);

    // JPQL subquery: find logs for the license whose licenseKey matches :key.
    // Use Pageable to limit results in a portable way.
    @Query("SELECT l FROM LicenseUsageLog l WHERE l.licenseId = (SELECT p.licenseId FROM ProductLicenses p WHERE p.licenseKey = :key) ORDER BY l.eventTime DESC")
    List<LicenseUsageLog> findByLicenseKey(@Param("key") String key, Pageable pageable);
}
