package com.cism.backend.model.users;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "otps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false)
    private String ipAddress;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required") @Email(message = "Email is invalid") private String email;
    
    @Column(nullable = false)
    @NotBlank(message = "OTP is required") private String otp;

    @Column(nullable = false)
    private Instant createAt;
}