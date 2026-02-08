package server.koraveler.translate.dto;

// LanguageStatistics.java

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageStatistics {
    private String sourceLanguage;
    private String targetLanguage;
    private Long count;
}

