package server.koraveler.users.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.Info;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Users {
    @Id
    private String id;
    private String title;
//    private String email;
//    private String password;
//    private List<String> roles = new ArrayList<>();
//    private List<? extends GrantedAuthority> authorities;
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return this.roles.stream()
//                .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
//    }
//    @Override
//    public String getUsername() {
//        return this.email;
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return this.isNonExpired();
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return this.isNonLocked();
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return this.isCredentialsNonExpired();
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return this.isNonExpired();
//    }
}
