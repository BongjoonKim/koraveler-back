package server.koraveler.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommonDTO {
    private LocalDateTime created;
    private LocalDateTime updated;
    private String createdUser;
    private String updatedUser;
    private String uniqKey;


}
