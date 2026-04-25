package com.cism.backend.model.stalls;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stall")
public class StallModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private long id;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StallMealsModel> mealList;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StallDrinksModel> drinkList;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StallSnacksModel> snackList;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Stall name is required") private String name;
 
    @Column(unique = false, nullable = true) private String description;

    @Column(unique = false, nullable = true) private String logo;

    @Column(unique = false, nullable = false) private String status;

    @Column(unique = false, nullable = false) private String openAt;

    @Column(unique = false, nullable = false) private String closeAt;

    @Column(nullable = false) private Instant createdAt;
    
}
