package com.cism.backend.repository.users;


import org.springframework.data.jpa.repository.JpaRepository;

import com.cism.backend.model.users.AuthModel;

import java.util.Optional;


public interface RegisterRepository extends JpaRepository<AuthModel, Integer> {
    Optional<AuthModel> findByEmail(String email);
    Optional<AuthModel> findByStudentId(String studentId);
    Optional<AuthModel> findByUsername(String username);
}
