package server.nadeliv.users.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequest {
    private String email;
}