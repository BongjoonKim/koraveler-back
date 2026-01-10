package server.koraveler.blog.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

public class DocumentView {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String documentId;

    private String userId; // 비로그인 시 null
    private String ipAddress;
    private String userAgent;
    private String referer;         // 유입 경로

    @Indexed
    private LocalDateTime viewedAt;

    // 통게용 필드
    private String country; // GeoIP로 추출
    private String deviceType; // mobile, desktop, tablet;
}
