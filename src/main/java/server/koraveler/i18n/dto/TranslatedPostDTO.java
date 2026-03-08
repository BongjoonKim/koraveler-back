package server.koraveler.i18n.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 독자용: locale 파라미터에 따라 번역된 글 응답
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatedPostDTO {
    private String id;
    private String originalLocale;
    private String currentLocale;

    @JsonProperty("isTranslated")
    private boolean isTranslated;

    private String translatedBy;  // "ai" | "human" | "ai+human" | null
    private String title;
    private String content;
    private String summary;
    private List<AvailableLocaleDTO> availableLocales;
}
