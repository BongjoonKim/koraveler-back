package server.koraveler.menus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.Common;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Menus extends Common {
    @Id
    private String id;
    private String label;
    private String value;
    private int sequence;
    private String url;
    private List<String> types;

}
