package server.koraveler.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenDTO  {
    private String refreshToken;
    private String accessToken;
    private Collection<? extends GrantedAuthority> grantType;
}
