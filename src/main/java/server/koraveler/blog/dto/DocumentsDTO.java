// DocumentsDTO.java - 별도 파일로 생성
package server.koraveler.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import server.koraveler.blog.model.Documents;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentsDTO extends Documents {
    // 내부 클래스가 아닌 독립 클래스로 분리
}