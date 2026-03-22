// TranslationRequest.java
package server.nadeliv.translate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {

    @NotBlank(message = "Source text is required")
    @Size(max = 5000, message = "Source text must not exceed 5000 characters")
    private String sourceText;

    @NotBlank(message = "Source language is required")
    @Pattern(regexp = "^(ko|en|ja|zh|es|fr|de|it|pt|ru|ar|hi)$",
            message = "Invalid source language code")
    private String sourceLanguage;

    @NotBlank(message = "Target language is required")
    @Pattern(regexp = "^(ko|en|ja|zh|es|fr|de|it|pt|ru|ar|hi)$",
            message = "Invalid target language code")
    private String targetLanguage;

    // 선택적: 번역 컨텍스트 (예: formal, informal, technical 등)
    private String context;

    // 선택적: 캐시 사용 여부
    private Boolean useCache = true;
}