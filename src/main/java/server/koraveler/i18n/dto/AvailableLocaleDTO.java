package server.koraveler.i18n.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableLocaleDTO {
    private String code;       // "ko", "en", "zh", "ja"
    private String name;       // "Korean", "English", ...
    private String nativeName; // "한국어", "English", ...
    private boolean isOriginal;
    private String status;     // "original" | "pending" | "translating" | "completed" | "failed" | "manually_edited"
}
