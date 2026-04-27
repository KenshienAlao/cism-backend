package com.cism.backend.repository.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cism.backend.model.admin.StallModel;

public interface CreateStallRepository extends JpaRepository<StallModel, Long> {
    Optional<StallModel> findById(Long id);
    Optional<StallModel> findByLicence(String licence);
    Optional<StallModel> findByLicenceAndPassword(String licence, String password);
}