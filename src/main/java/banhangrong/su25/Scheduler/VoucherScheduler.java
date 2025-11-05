package banhangrong.su25.Scheduler;

import banhangrong.su25.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class VoucherScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VoucherScheduler.class);

    private final VoucherService voucherService;

    public VoucherScheduler(VoucherService voucherService) {
        this.voucherService = voucherService;
    }


    /**
     * Auto-expire vouchers - Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void autoExpireVouchersDaily() {
        try {
            logger.info("🕐 Starting daily auto-expire vouchers job");
            int expiredCount = voucherService.autoExpireVouchers();
            logger.info("✅ Daily job: Auto-expired {} vouchers", expiredCount);
        } catch (Exception e) {
            logger.error("❌ Error during daily auto-expire vouchers job", e);
        }
    }

    /**
     * Auto-expire vouchers - Runs every hour (for more frequent checks)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoExpireVouchersHourly() {
        try {
            logger.debug("⏰ Starting hourly auto-expire vouchers check");
            int expiredCount = voucherService.autoExpireVouchers();
            if (expiredCount > 0) {
                logger.info("✅ Hourly check: Auto-expired {} vouchers", expiredCount);
            }
        } catch (Exception e) {
            logger.error("❌ Error during hourly auto-expire vouchers check", e);
        }
    }
}

