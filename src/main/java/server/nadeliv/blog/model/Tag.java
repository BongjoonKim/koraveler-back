package server.nadeliv.blog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.common.dto.CommonDTO;

@Document(collection = "tags")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tag extends CommonDTO {
    @Id
    private String id; // Tag 아이디

    @Indexed
    private String tagName; // 태그명

    @Indexed
    private String slug;    // URL용

    private int usageCount; // 사용 횟수

    private String color; // 랜덤하게 설정
}
