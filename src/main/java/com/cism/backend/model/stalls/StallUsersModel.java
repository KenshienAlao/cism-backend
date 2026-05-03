package com.cism.backend.model.stalls;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.cism.backend.model.admin.StallModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stall_users")
public class StallUsersModel {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        @ManyToOne
        @OnDelete(action = OnDeleteAction.CASCADE)
        @JoinColumn(name = "stall_id", nullable = false)
        private StallModel stall;

        @Column(nullable = false)
        private String name;
        @Column(nullable = true)
        private String description;
        @Column(nullable = true)
        private Boolean status;
        @Column(nullable = false)
        private String openAt;
        @Column(nullable = false)
        private String closeAt;
        @Column(nullable = true)
        private String image;
        @Column(nullable = false)
        private String role;

        @UpdateTimestamp
        @Column(nullable = false)
        private Instant updatedAt;

        @CreationTimestamp
        @Column(nullable = false)
        private Instant createdAt;

}
