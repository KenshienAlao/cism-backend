package com.cism.backend.model.stalls;

import java.time.Instant;

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

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stall_meals")
public class StallMealsModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private long id;

    @ManyToOne
    @JoinColumn(name = "stall_id", nullable = false) private StallModel stall;

    @Column(unique = false, nullable = false) private String name;

    @Column(unique = false, nullable = false) private String price;

    @Column(unique = false, nullable = false) private String stocks;
    
    @Column(unique = false, nullable = false) private String image;

    @Column(unique = false, nullable = false) private Instant createdAt;

    @Column(unique = false, nullable = false) private Instant updatedAt;
    
}
