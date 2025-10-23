package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ShopLicenses;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShopLicensesRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/seller/licenses")
public class SellerLicenseController {

    @Autowired
    private ShopLicensesRepository shopLicensesRepository;

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> getLicenses(HttpSession session) {
        Users currentUser = (Users) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<ShopLicenses> licenses = shopLicensesRepository.findBySellerIdOrderByCreatedAtDesc(currentUser.getUserId());
        return ResponseEntity.ok(licenses);
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addLicense(@RequestBody Map<String, String> data, HttpSession session) {
        Users currentUser = (Users) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            ShopLicenses license = new ShopLicenses();
            license.setSellerId(currentUser.getUserId());
            license.setLicenseName(data.get("licenseName"));
            license.setLicenseType(data.get("licenseType"));
            license.setLicenseNumber(data.get("licenseNumber"));
            license.setDescription(data.get("description"));

            // Parse dates if provided
            if (data.get("issueDate") != null && !data.get("issueDate").isEmpty()) {
                license.setIssueDate(LocalDateTime.parse(data.get("issueDate") + "T00:00:00"));
            }
            if (data.get("expiryDate") != null && !data.get("expiryDate").isEmpty()) {
                license.setExpiryDate(LocalDateTime.parse(data.get("expiryDate") + "T00:00:00"));
            }

            license.setStatus(data.getOrDefault("status", "ACTIVE"));
            license.setIsActive(true);

            ShopLicenses saved = shopLicensesRepository.save(license);
            return ResponseEntity.ok(Map.of("success", true, "license", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add license: " + e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateLicense(@PathVariable Long id, @RequestBody Map<String, String> data, HttpSession session) {
        Users currentUser = (Users) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            ShopLicenses license = shopLicensesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("License not found"));

            if (!license.getSellerId().equals(currentUser.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            license.setLicenseName(data.get("licenseName"));
            license.setLicenseType(data.get("licenseType"));
            license.setLicenseNumber(data.get("licenseNumber"));
            license.setDescription(data.get("description"));

            if (data.get("issueDate") != null && !data.get("issueDate").isEmpty()) {
                license.setIssueDate(LocalDateTime.parse(data.get("issueDate") + "T00:00:00"));
            }
            if (data.get("expiryDate") != null && !data.get("expiryDate").isEmpty()) {
                license.setExpiryDate(LocalDateTime.parse(data.get("expiryDate") + "T00:00:00"));
            }

            if (data.containsKey("status")) {
                license.setStatus(data.get("status"));
            }

            ShopLicenses updated = shopLicensesRepository.save(license);
            return ResponseEntity.ok(Map.of("success", true, "license", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update license: " + e.getMessage()));
        }
    }

    @PutMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleStatus(@PathVariable Long id, HttpSession session) {
        Users currentUser = (Users) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            ShopLicenses license = shopLicensesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("License not found"));

            if (!license.getSellerId().equals(currentUser.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            license.setIsActive(!license.getIsActive());
            license.setStatus(license.getIsActive() ? "ACTIVE" : "INACTIVE");

            ShopLicenses updated = shopLicensesRepository.save(license);
            return ResponseEntity.ok(Map.of("success", true, "license", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to toggle status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteLicense(@PathVariable Long id, HttpSession session) {
        Users currentUser = (Users) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            ShopLicenses license = shopLicensesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("License not found"));

            if (!license.getSellerId().equals(currentUser.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            shopLicensesRepository.delete(license);
            return ResponseEntity.ok(Map.of("success", true, "message", "License deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete license: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<?> getLicenseStats(HttpSession session) {
        Users currentUser = (Users) session.getAttribute("user");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        long totalLicenses = shopLicensesRepository.findBySellerIdOrderByCreatedAtDesc(currentUser.getUserId()).size();
        long activeLicenses = shopLicensesRepository.countBySellerIdAndIsActive(currentUser.getUserId(), true);
        long inactiveLicenses = totalLicenses - activeLicenses;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalLicenses);
        stats.put("active", activeLicenses);
        stats.put("inactive", inactiveLicenses);

        return ResponseEntity.ok(stats);
    }
}

