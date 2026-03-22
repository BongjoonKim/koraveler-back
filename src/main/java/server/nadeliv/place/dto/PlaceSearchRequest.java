// PlaceSearchRequest.java
package server.nadeliv.place.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchRequest {

    @NotBlank(message = "Search keyword is required")
    private String keyword;
}
