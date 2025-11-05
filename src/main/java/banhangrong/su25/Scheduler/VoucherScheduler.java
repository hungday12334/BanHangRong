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


    @Scheduled(cron = "0 0 0 * * ?")
    public void autoExpireVouchers() {
        try {
            logger.info("Starting auto-expire vouchers job");
            int expiredCount = voucherService.autoExpireVouchers();
            logger.info("Auto-expired {} vouchers", expiredCount);
        } catch (Exception e) {
            logger.error("Error during auto-expire vouchers job", e);
        }
    }
}

