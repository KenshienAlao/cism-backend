package com.cism.backend.model.stalls;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "stall_items")
public class StallItemModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "stall_id", nullable = false)
    private StallModel stall;

    @Column(unique = false, nullable = false)
    private String name;

    @Column(unique = false, nullable = false)
    private BigDecimal price;

    @Column(unique = false, nullable = false)
    private Integer stocks;

    @Column(unique = false, nullable = true)
    private String image;

    @Column(unique = false, nullable = false)
    private String category;

    @Column(unique = false, nullable = false)
    private Integer sold;

    @Column(unique = false, nullable = false)
    private Integer previousSold;

    @Column(unique = false, nullable = false)
    private Instant createdAt;

    @Column(unique = false, nullable = false)
    private Instant updatedAt;

}
