package com.cism.backend.model.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class AuthModel implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private long id;
    
    @Column(unique = false, nullable = false)
    @NotBlank(message = "Username is Required")
    private String username;

    @Column(unique = true, nullable = true)
    private String studentId;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is Required")
    private String email;

    @Column(unique = false, nullable = false)
    @NotBlank(message = "Password is Required") 
    private String password;    

    @Column(unique = false, nullable = true)
    private String role;

    @Column(unique = false, nullable = true)
    private String avatar;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role == null ? "ROLE_USER" : role));
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