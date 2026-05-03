package com.cism.backend.model.admin;

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

import org.hibernate.annotations.CreationTimestamp;

import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.model.stalls.StallIncomesModel;
import com.cism.backend.model.stalls.StallUsersModel;
import com.cism.backend.model.system.review.ReviewModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stall")
public class StallModel implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StallItemModel> itemList;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StallUsersModel> userList;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ReviewModel> reviewList;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StallIncomesModel> incomeList;

    @Column(unique = true, nullable = false)
    private String licence;
    @Column(unique = true, nullable = false)
    private String password;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String userRole = (userList != null && !userList.isEmpty() && userList.get(0).getRole() != null)
                ? userList.get(0).getRole()
                : "STALL";
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole));
    }

    @Override
    public String getUsername() {
        return this.licence;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
