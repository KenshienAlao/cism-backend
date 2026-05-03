package com.cism.backend.service.stalls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cism.backend.repository.stalls.StallItemRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TrendingService {

    @Autowired
    private StallItemRepository stallItemRepository;

    /**
     * Snapshots the current 'sold' count into 'previousSold' and resets 'sold' to 0.
     * This is scheduled to run once a week (every Sunday at midnight).
     * Cron: 0 0 0 * * SUN
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void snapshotWeeklySales() {
        log.info("Starting weekly sales snapshot for all items...");

        stallItemRepository.findAll().forEach(item -> {
            item.setPreviousSold(item.getSold());
            item.setSold(0);
            stallItemRepository.save(item);
        });

        log.info("Weekly sales snapshot completed.");
    }

    /**
     * Manual snapshot for testing purposes.
     */
    @Transactional
    public void manualSnapshot() {
        snapshotWeeklySales();
    }
}
