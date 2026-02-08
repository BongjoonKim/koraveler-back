# Koraveler Backend 개발 가이드

## 1. 프로젝트 개요
한국 여행 블로그 플랫폼 Koraveler 백엔드. Google AdSense 수익화 목표.

## 2. 기술 스택
- Spring Boot 3.2.5
- MongoDB (Spring Data MongoDB) v5
- Spring Security + JWT (stateless)
- Redis (ElastiCache/Local)
- AWS (EC2, S3, Lambda, CloudWatch)

---

## 3. 프로젝트 구조
```
src/main/java/server/koraveler/
├── config/                     # 설정 클래스
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── RedisConfig.java
│   └── AppConfig.java
├── common/
│   └── dto/
│       ├── CommonDTO.java      # created, updated, createdUser, updatedUser
│       └── UserCommon.java     # 사용자 관련 공통 필드
├── blog/
│   ├── model/                  # MongoDB Document
│   ├── dto/                    # 데이터 전송 객체
│   ├── repo/                   # Repository
│   ├── service/                # Service Interface
│   │   └── serviceImpl/        # Service 구현체
│   └── controller/             # REST Controller
├── users/
│   ├── model/
│   ├── dto/
│   ├── repo/
│   ├── service/
│   ├── controller/
│   └── component/              # JWT Filter 등
├── connections/                # 연결 관계 (북마크 등)
├── utils/                      # 유틸리티 (JwtUtil 등)
└── error/                      # 예외 처리
```

---

## 4. 코딩 컨벤션

### 4.1 Model (MongoDB Document)
```java
package server.koraveler.blog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import server.koraveler.common.dto.CommonDTO;

@Document(collection = "entity_names")  // 복수형 스네이크케이스
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityName extends CommonDTO {
    @Id
    private String id;
    
    // 외래키 참조 시 ObjectId 타입 + 인덱스
    @Field(targetType = FieldType.OBJECT_ID)
    @Indexed
    private String documentId;
    
    @Indexed
    private String userId;
    
    // 비정규화된 count 필드 (성능 최적화)
    private int likeCount;
    private int replyCount;
    
    // boolean 필드는 is 접두사 사용
    private boolean isDeleted;
    private boolean isEdited;
}
```

**CommonDTO 상속 필드:**
- `LocalDateTime created`
- `LocalDateTime updated`
- `String createdUser`
- `String updatedUser`

### 4.2 DTO
```java
package server.koraveler.blog.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDTO {
    private String id;
    private String documentId;
    private String userId;
    private String content;
    
    // 응답 전용 필드 (DB에 없음)
    private boolean isLiked;      // 현재 사용자의 좋아요 여부
    private long likeCount;       // 좋아요 수
    private boolean amIWriter;    // 현재 사용자가 작성자인지
    private List<EntityDTO> replies;  // 하위 항목
    
    // CommonDTO 필드
    private LocalDateTime created;
    private LocalDateTime updated;
    private String createdUser;
    private String updatedUser;
}
```

### 4.3 Repository
```java
package server.koraveler.blog.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.koraveler.blog.model.Entity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityRepo extends MongoRepository<Entity, String> {
    
    // ⚠️ 중요: boolean 필드 조회 시 "is" 접두사 생략!
    // 모델: isDeleted → 메서드: DeletedFalse (IsDeletedFalse ❌)
    Page<Entity> findByDocumentIdAndDepthAndDeletedFalse(
        String documentId, int depth, Pageable pageable);
    
    // 단일 조회
    Optional<Entity> findByDocumentIdAndUserId(String documentId, String userId);
    
    // 존재 여부 확인
    boolean existsByDocumentIdAndUserId(String documentId, String userId);
    
    // 삭제
    void deleteByDocumentIdAndUserId(String documentId, String userId);
    
    // 카운트
    long countByDocumentId(String documentId);
    long countByParentIdAndDeletedFalse(String parentId);
    
    // 벌크 조회 (N+1 방지)
    List<Entity> findByIdInAndUserId(List<String> ids, String userId);
    List<Entity> findByDocumentIdInAndUserId(List<String> documentIds, String userId);
    
    // 정렬 포함
    List<Entity> findByParentIdAndDeletedFalseOrderByCreatedAsc(String parentId);
    
    // 커스텀 쿼리
    @Query("{ 'documentId': ?0, 'depth': 1 }")
    List<Entity> findDepth1ByDocumentId(String documentId);
}
```

### 4.4 Service Interface
```java
package server.koraveler.blog.service;

import org.springframework.data.domain.Pageable;
import server.koraveler.blog.dto.EntityDTO;
import server.koraveler.blog.dto.EntityPageDTO;

import java.util.List;

public interface EntityService {
    // 생성
    EntityDTO create(EntityDTO dto, String userId) throws Exception;
    
    // 수정
    EntityDTO update(String id, EntityDTO dto, String userId) throws Exception;
    
    // 삭제 (soft delete)
    void delete(String id, String userId) throws Exception;
    
    // 단일 조회
    EntityDTO getById(String id, String userId) throws Exception;
    
    // 목록 조회 (페이징)
    EntityPageDTO getList(String parentId, String userId, Pageable pageable) throws Exception;
    
    // 토글 (좋아요 등)
    EntityDTO toggle(String targetId, String userId) throws Exception;
    
    // 벌크 조회
    List<String> getLikedIds(List<String> targetIds, String userId);
}
```

### 4.5 Service Implementation
```java
package server.koraveler.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.EntityDTO;
import server.koraveler.blog.model.Entity;
import server.koraveler.blog.repo.EntityRepo;
import server.koraveler.blog.service.EntityService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor  // 생성자 주입 (final 필드)
public class EntityServiceImpl implements EntityService {
    
    private final EntityRepo entityRepo;
    private final RelatedService relatedService;  // 연관 서비스
    
    @Override
    public EntityDTO create(EntityDTO dto, String userId) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 유효성 검증
        if (dto.getDocumentId() == null) {
            throw new Exception("documentId is required");
        }
        
        // 2. Entity 생성
        Entity entity = Entity.builder()
                .documentId(dto.getDocumentId())
                .userId(userId)
                .content(dto.getContent())
                .isDeleted(false)
                .isEdited(false)
                .build();
        
        // CommonDTO 필드 설정
        entity.setCreated(now);
        entity.setUpdated(now);
        entity.setCreatedUser(userId);
        entity.setUpdatedUser(userId);
        
        // 3. 저장
        Entity saved = entityRepo.save(entity);
        
        // 4. DTO 변환 후 반환
        EntityDTO result = convertToDTO(saved, userId);
        result.setAmIWriter(true);
        
        log.info("Entity 생성 완료: id={}, userId={}", saved.getId(), userId);
        
        return result;
    }
    
    @Override
    public EntityDTO update(String id, EntityDTO dto, String userId) throws Exception {
        Entity entity = entityRepo.findById(id)
                .orElseThrow(() -> new Exception("Entity를 찾을 수 없습니다."));
        
        // 작성자 확인
        if (!entity.getUserId().equals(userId)) {
            throw new Exception("수정 권한이 없습니다.");
        }
        
        // 삭제된 항목은 수정 불가
        if (entity.isDeleted()) {
            throw new Exception("삭제된 항목은 수정할 수 없습니다.");
        }
        
        // 업데이트
        entity.setContent(dto.getContent());
        entity.setEdited(true);
        entity.setUpdated(LocalDateTime.now());
        entity.setUpdatedUser(userId);
        
        Entity updated = entityRepo.save(entity);
        
        EntityDTO result = convertToDTO(updated, userId);
        result.setAmIWriter(true);
        
        log.info("Entity 수정 완료: id={}", id);
        
        return result;
    }
    
    @Override
    public void delete(String id, String userId) throws Exception {
        Entity entity = entityRepo.findById(id)
                .orElseThrow(() -> new Exception("Entity를 찾을 수 없습니다."));
        
        // 작성자 확인
        if (!entity.getUserId().equals(userId)) {
            throw new Exception("삭제 권한이 없습니다.");
        }
        
        // Soft delete
        entity.setDeleted(true);
        entity.setUpdated(LocalDateTime.now());
        entity.setUpdatedUser(userId);
        
        entityRepo.save(entity);
        
        log.info("Entity 삭제 완료: id={}", id);
    }
    
    // Entity -> DTO 변환 헬퍼
    private EntityDTO convertToDTO(Entity entity, String userId) {
        EntityDTO dto = new EntityDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 작성자 여부
        dto.setAmIWriter(userId != null && userId.equals(entity.getUserId()));
        
        // 좋아요 여부 및 수
        if (userId != null) {
            dto.setLiked(relatedService.hasLiked(entity.getId(), userId));
        }
        dto.setLikeCount(relatedService.getLikeCount(entity.getId()));
        
        return dto;
    }
}
```

### 4.6 Controller
```java
package server.koraveler.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.blog.dto.EntityDTO;
import server.koraveler.blog.service.EntityService;
import server.koraveler.users.dto.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Slf4j
public class EntityController {
    
    private final EntityService entityService;
    
    /**
     * 생성 (로그인 필수)
     */
    @PostMapping("")
    public ResponseEntity<?> create(
            @RequestBody EntityDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            EntityDTO created = entityService.create(dto, userId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("생성 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 수정 (로그인 필수, 작성자만)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody EntityDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            EntityDTO updated = entityService.update(id, dto, userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("수정 실패: id={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("수정 실패: " + e.getMessage());
        }
    }
    
    /**
     * 삭제 (로그인 필수, 작성자만)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            entityService.delete(id, userId);
            return ResponseEntity.ok("삭제 완료");
        } catch (Exception e) {
            log.error("삭제 실패: id={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("삭제 실패: " + e.getMessage());
        }
    }
    
    /**
     * 목록 조회 (비로그인 허용)
     * ps = public service
     */
    @GetMapping("/ps/list/{parentId}")
    public ResponseEntity<?> getList(
            @PathVariable String parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            // 비로그인은 userDetails가 null
            String userId = (userDetails != null) ? userDetails.getUsername() : null;
            
            Sort sortOrder = "asc".equalsIgnoreCase(sort)
                    ? Sort.by(Sort.Direction.ASC, "created")
                    : Sort.by(Sort.Direction.DESC, "created");
            Pageable pageable = PageRequest.of(page, size, sortOrder);
            
            return ResponseEntity.ok(entityService.getList(parentId, userId, pageable));
        } catch (Exception e) {
            log.error("목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 단일 조회 (비로그인 허용)
     */
    @GetMapping("/ps/{id}")
    public ResponseEntity<?> getById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = (userDetails != null) ? userDetails.getUsername() : null;
            return ResponseEntity.ok(entityService.getById(id, userId));
        } catch (Exception e) {
            log.error("조회 실패: id={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 토글 (좋아요 등) - 로그인 필수
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            return ResponseEntity.ok(entityService.toggle(id, userId));
        } catch (Exception e) {
            log.error("토글 실패: id={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("토글 실패: " + e.getMessage());
        }
    }
}
```

---

## 5. 인증 처리

### 5.1 사용자 정보 가져오기
```java
// ✅ 권장: @AuthenticationPrincipal 사용
@PostMapping("")
public ResponseEntity<?> create(
        @RequestBody EntityDTO dto,
        @AuthenticationPrincipal CustomUserDetails userDetails
) {
    String userId = userDetails.getUsername();  // userId 획득
    Users user = userDetails.getUsers();        // Users 엔티티 전체
}

// ❌ 비권장: SecurityContextHolder 직접 사용
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
UserDetails userDetails = (UserDetails) auth.getPrincipal();
```

### 5.2 비로그인 허용 엔드포인트
```java
// userDetails가 null일 수 있음
@GetMapping("/ps/{id}")
public ResponseEntity<?> get(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails
) {
    String userId = (userDetails != null) ? userDetails.getUsername() : null;
    // userId가 null이면 좋아요 여부 등은 false로 처리
}
```

---

## 6. URL 패턴

### 인증 필요 (JWT 토큰 필수)
```
POST   /api/v1/entities              - 생성
PUT    /api/v1/entities/{id}         - 수정
DELETE /api/v1/entities/{id}         - 삭제
PATCH  /api/v1/entities/{id}/action  - 부분 수정 (숨김, 공개 등)
POST   /api/v1/entities/{id}/like    - 좋아요 토글
```

### 비인증 허용 (ps = public service)
```
GET    /api/v1/entities/ps/{id}           - 단일 조회
GET    /api/v1/entities/ps/list/{parentId} - 목록 조회
GET    /api/v1/entities/ps/document/{id}  - 특정 문서의 목록
POST   /api/v1/views/ps/{documentId}      - 조회수 증가 (IP 기반)
```

### SecurityConfig 설정
```java
.authorizeHttpRequests(req -> req
    .requestMatchers(new ContainsPsRequestMatcher()).permitAll()
    .requestMatchers(HttpMethod.POST, "/api/v1/views/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/views/**").permitAll()
    .anyRequest().authenticated()
)
```

---

## 7. Redis 활용

### 7.1 조회수 중복 방지
```java
private static final String VIEW_CHECK_PREFIX = "view:check:";
private static final Duration DUPLICATE_PREVENTION_TTL = Duration.ofHours(24);

// Redis Key: view:check:{documentId}:{ipAddress}
String checkKey = VIEW_CHECK_PREFIX + documentId + ":" + ipAddress;

Boolean isFirstView = redisTemplate.opsForValue().setIfAbsent(
        checkKey,
        "1",
        DUPLICATE_PREVENTION_TTL
);
```

### 7.2 캐시 TTL 전략
| 용도 | TTL | 설명 |
|------|-----|------|
| 조회수 카운트 | 5분 | 일반 통계 |
| 실시간 데이터 | 1분 | 자주 변경되는 데이터 |
| 중복 방지 | 24시간 | IP 기반 조회수 |

---

## 8. 주의사항

### 8.1 Boolean 필드 쿼리 규칙
```java
// 모델 필드
private boolean isDeleted;

// Repository 메서드 ⚠️
// ✅ 올바른 방법: "is" 생략
Page<Entity> findByDocumentIdAndDeletedFalse(String documentId, Pageable pageable);

// ❌ 잘못된 방법
Page<Entity> findByDocumentIdAndIsDeletedFalse(...);  // 동작 안 함!
```

### 8.2 N+1 방지
```java
// ❌ N+1 발생
for (Entity entity : entities) {
    boolean liked = likeRepo.existsByEntityIdAndUserId(entity.getId(), userId);
}

// ✅ 벌크 조회
List<String> entityIds = entities.stream().map(Entity::getId).toList();
List<Like> likes = likeRepo.findByEntityIdInAndUserId(entityIds, userId);
Set<String> likedIds = likes.stream().map(Like::getEntityId).collect(toSet());
```

### 8.3 비정규화 활용
```java
// 좋아요 수를 원본 문서에 저장 (빠른 조회)
@Document(collection = "comments")
public class Comment {
    private int likeCount;  // 비정규화
}

// 좋아요 토글 시 양쪽 업데이트
public void toggle(String commentId, String userId) {
    // likes 컬렉션 업데이트
    // comments.likeCount 증가/감소
}
```

### 8.4 Soft Delete
```java
// 완전 삭제 대신 플래그 사용
entity.setDeleted(true);
entity.setUpdated(LocalDateTime.now());
entityRepo.save(entity);

// 조회 시 삭제되지 않은 것만
Page<Entity> findByParentIdAndDeletedFalse(String parentId, Pageable pageable);
```

---

## 9. 개발 순서

**Backend First 원칙:**
1. Model 작성 (MongoDB Document)
2. DTO 작성
3. Repository 작성
4. Service Interface 작성
5. ServiceImpl 작성
6. Controller 작성
7. SecurityConfig에 URL 패턴 추가
8. 테스트

---

## 10. 에러 처리
```java
// Service에서 Exception throw
if (!entity.getUserId().equals(userId)) {
    throw new Exception("권한이 없습니다.");
}

// Controller에서 catch 후 적절한 응답
try {
    return ResponseEntity.ok(service.update(id, dto, userId));
} catch (Exception e) {
    log.error("수정 실패: id={}", id, e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("수정 실패: " + e.getMessage());
}
```