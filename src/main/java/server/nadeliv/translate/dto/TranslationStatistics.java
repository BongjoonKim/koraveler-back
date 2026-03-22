// TranslationStatistics.java
package server.nadeliv.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationStatistics {
    private Long totalTranslations;
    private Long likedTranslations;
    private String mostUsedSourceLanguage;
    private String mostUsedTargetLanguage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double averageTranslationsPerDay;

    public Double getAverageTranslationsPerDay() {
        if (startDate != null && endDate != null && totalTranslations != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            return totalTranslations.doubleValue() / days;
        }
        return 0.0;
    }
}