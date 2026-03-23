package server.nadeliv.users.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCodeVerifyRequest {
    private String email;
    private String code;
}