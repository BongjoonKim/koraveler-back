package server.koraveler.menus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import server.koraveler.common.dto.common;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Menus extends common {
    @Id
    private String id;
    private String label;
    private String value;
    private int sequence;
    private String url;

}
