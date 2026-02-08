// QuickPhrase.java
package server.koraveler.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickPhrase {
    private String text;           // 원문
    private String translation;    // 번역
    private String pronunciation;  // 발음
    private String category;       // 카테고리
}