package com.cism.backend.repository.users;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.cism.backend.model.users.OtpModel;

public interface OtpRepository extends JpaRepository<OtpModel, Long> {
    Optional<OtpModel> findByEmail(String email);
    Optional<OtpModel> findTopByIpAddressOrderByCreateAtDesc(String ipString);
    @Modifying
    @Transactional
    void deleteByEmail(String email);
    long countByIpAddressAndCreateAtAfter(String ipAddress, Instant createAt);

    @Modifying
    @Transactional
    void deleteByCreateAtBefore(Instant cutoff);
}
