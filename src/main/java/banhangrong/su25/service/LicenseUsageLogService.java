package banhangrong.su25.service;

import banhangrong.su25.Entity.LicenseUsageLog;
import banhangrong.su25.Repository.LicenseUsageLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LicenseUsageLogService {
    private final LicenseUsageLogRepository repo;

    public LicenseUsageLogService(LicenseUsageLogRepository repo) { this.repo = repo; }

    /** Return latest N logs by license id */
    public List<LicenseUsageLog> getLatestByLicenseId(Long licenseId, int limit) {
        if (licenseId == null) return List.of();
        return repo.findByLicenseIdOrderByEventTimeDesc(licenseId, PageRequest.of(0, Math.max(1, limit)));
    }

    /** Return a page of logs by license id */
    public org.springframework.data.domain.Page<LicenseUsageLog> getLogsPageByLicenseId(Long licenseId, int page, int size) {
        if (licenseId == null) return org.springframework.data.domain.Page.empty();
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(size, 100));
        return repo.findPageByLicenseIdOrderByEventTimeDesc(licenseId, PageRequest.of(p, s));
    }

    /** Return latest N logs by license key */
    public List<LicenseUsageLog> getLatestByLicenseKey(String key, int limit) {
        if (key == null || key.isBlank()) return List.of();
        // Use Pageable to request the latest N entries in a portable way
        return repo.findByLicenseKey(key.trim(), PageRequest.of(0, Math.max(1, limit)));
    }

    /** Append a log record (used by other parts of app when license is used) */
    public LicenseUsageLog append(Long licenseId, String action, Long userId, String ip, String device, String meta) {
        LicenseUsageLog l = new LicenseUsageLog();
        l.setLicenseId(licenseId);
        l.setAction(action != null ? action : "event");
        l.setUserId(userId);
        l.setIp(ip);
        l.setDevice(device);
        l.setMeta(meta);
        LocalDateTime now = LocalDateTime.now();
        l.setEventTime(now);
        l.setCreatedAt(now);
        return repo.save(l);
    }
}
