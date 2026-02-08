package server.koraveler.users.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import server.koraveler.common.dto.UserCommon;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Users extends UserCommon {
    @Id
    private String id;
    private String userId;
    private String userPassword;
    private String email;
    private String password;
    private String src;
    private String name;
    private LocalDateTime birthday;

    @Builder.Default
    private List<String> roles = new ArrayList<>(List.of("user"));  // 기본 권한 설정

    @Builder.Default
    private List<String> authorities = new ArrayList<>(List.of("user"));  // 기본 권한 설정

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // 빌더 사용 시 부모 클래스 필드 초기화를 위한 메서드
    @Builder
    public Users(String id, String userId, String userPassword, String email,
                 String password, String src, String name, LocalDateTime birthday,
                 List<String> roles, List<String> authorities,
                 LocalDateTime created, LocalDateTime updated,
                 Boolean isNonExpired, Boolean isNonLocked,
                 Boolean isCredentialsNonExpired, Boolean isEnabled) {

        super();  // 부모 클래스의 기본값 설정

        this.id = id;
        this.userId = userId;
        this.userPassword = userPassword;
        this.email = email;
        this.password = password;
        this.src = src;
        this.name = name;
        this.birthday = birthday;

        // roles 기본값 처리
        this.roles = (roles != null && !roles.isEmpty())
                ? roles
                : new ArrayList<>(List.of("user"));

        // authorities 기본값 처리
        this.authorities = (authorities != null && !authorities.isEmpty())
                ? authorities
                : new ArrayList<>(List.of("user"));

        // 부모 클래스 필드 설정 (null이면 기본값 사용)
        this.setCreated(created != null ? created : LocalDateTime.now());
        this.setUpdated(updated != null ? updated : LocalDateTime.now());
        this.setNonExpired(isNonExpired != null ? isNonExpired : true);
        this.setNonLocked(isNonLocked != null ? isNonLocked : true);
        this.setCredentialsNonExpired(isCredentialsNonExpired != null ? isCredentialsNonExpired : true);
        this.setEnabled(isEnabled != null ? isEnabled : true);
    }
}