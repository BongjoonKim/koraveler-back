package server.koraveler.users.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import server.koraveler.users.model.Users;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private Users users;

    public CustomUserDetails(Users users) {
        this.users = users;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return users.getUserPassword();
    }

    @Override
    public String getUsername() {
        return users.getUserId();
    }

    @Override
    public boolean isEnabled() {
        return users.isEnabled();
    }

    @Override
    public boolean isAccountNonExpired() {
        return users.isNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return users.isNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return users.isCredentialsNonExpired();
    }
}
