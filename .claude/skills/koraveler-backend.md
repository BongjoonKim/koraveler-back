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
├── travel/
│   ├── model/
│   │   ├── entities/           # Travels, TravelUsers, TravelMedia, TravelChannel
│   │   ├── dto/                # Request/Response DTO (Travel*, TravelChannel*)
│   │   ├── enums/              # TravelRole, TravelStatus, TravelVisibility, ChannelContextType
│   │   ├── embedded/           # TravelSchedule (내장 문서)
│   │   └── mapper/             # TravelMapper, TravelChannelMapper
│   ├── repo/                   # TravelsRepo, TravelUsersRepo, TravelMediaRepo, TravelChannelRepo
│   ├── service/
│   │   └── serviceImpl/        # TravelServiceImpl, TravelChannelServiceImpl
│   └── controller/             # TravelController, TravelChannelController
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

### 10.1 CustomException + ErrorCode (권장 — Travel, Chat 모듈)
```java
// ErrorCode enum 정의 (server.koraveler.error.ErrorCode)
TRAVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "TRV_001", "여행 프로젝트를 찾을 수 없습니다"),
NOT_TRAVEL_MEMBER(HttpStatus.NOT_FOUND, "TRV_006", "여행 멤버가 아닙니다"),
TRAVEL_CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "TRV_015", "여행 채널을 찾을 수 없습니다"),
CHANNEL_ALREADY_LINKED(HttpStatus.CONFLICT, "TRV_016", "이미 연결된 채널입니다"),

// Service에서 throw
throw new CustomException(ErrorCode.TRAVEL_NOT_FOUND);
throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER_MANAGE);

// CustomException 필드: status, code, msg, detail (getErrorCode() 없음)
// 에러 코드 비교 시:
if (ErrorCode.ALREADY_CHANNEL_MEMBER.getCode().equals(e.getCode())) { ... }

// Controller에서는 try-catch 없이 GlobalExceptionHandler가 처리
@PostMapping
public ResponseEntity<TravelChannelResponse> createChannel(...) {
    return ResponseEntity.ok(service.createChannel(travelId, request, userId));
}
```

### 10.2 레거시 방식 (Blog 모듈)
```java
// Service에서 Exception throw
throw new Exception("권한이 없습니다.");

// Controller에서 catch 후 적절한 응답
try {
    return ResponseEntity.ok(service.update(id, dto, userId));
} catch (Exception e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("수정 실패: " + e.getMessage());
}
```

---

## 11. Travel (여행 프로젝트) 모듈

### 11.1 개요
여행 계획을 그룹으로 관리하는 기능. 멤버 초대, 일정 관리, 채널 연결, 미디어 업로드 지원.
Base Path: `/api/v1/travels` (모든 API 인증 필수)

### 11.2 Enum
```java
public enum TravelVisibility { PUBLIC, PRIVATE }
public enum TravelStatus { PLANNING, IN_PROGRESS, COMPLETED, CANCELLED }
public enum TravelRole { ADMIN, USER }
public enum ChannelContextType { GENERAL, ITINERARY, PLACE, MEDIA, INFO }
```

### 11.3 Entity 구조

**Travels** (collection: `travels`) - CommonDTO 상속
- `id`, `title`, `description`, `coverImageUrl`
- `visibility` (TravelVisibility), `status` (TravelStatus)
- `startDate`, `endDate` (LocalDate), `destination`
- `tags` (List<String>)
- `schedules` (List<TravelSchedule>) — 내장 문서
- `channelIds` (List<String>)

**TravelUsers** (collection: `travel_users`) - CommonDTO 상속
- `id`, `travelId` (@Indexed), `userId` (@Indexed)
- `role` (TravelRole), `nickname`, `joinedAt` (LocalDateTime)

**TravelMedia** (collection: `travel_media`) - CommonDTO 상속
- `id`, `travelId` (@Indexed), `uploadUserId`
- `fileName`, `originalFileName`, `fileUrl`, `thumbnailUrl`
- `mimeType`, `fileSize` (Long), `width`, `height`, `duration` (Integer)
- `description`, `takenAt` (LocalDateTime)

**TravelChannel** (collection: `travel_channels`) - CommonDTO 상속, 브릿지 모델
- `id`, `travelId` (@Indexed), `channelId` (@Indexed)
- `contextType` (ChannelContextType), `contextId` (nullable)
- `channelPurpose` (String)
- `isPinned` (Boolean, default false), `displayOrder` (Integer, default 0)
- `isDeleted` (Boolean, default false) — Soft Delete

**TravelSchedule** (내장 문서, 별도 컬렉션 없음)
- `id`, `dayNumber` (Integer), `date` (LocalDate)
- `title`, `description`, `sortOrder` (Integer)
- `places` (List<SchedulePlace>)
  - SchedulePlace: `name`, `address`, `lat` (Double), `lng` (Double), `memo`

### 11.4 DTO

**TravelCreateRequest** — title 필수(max 100), description(max 2000), coverImageUrl, visibility, startDate, endDate, destination, tags
**TravelUpdateRequest** — title(max 100), description(max 2000), coverImageUrl, visibility, status, startDate, endDate, destination, tags
**TravelMemberRequest** — userId 필수, role, nickname
**TravelScheduleRequest** — title 필수, dayNumber, date, description, places, sortOrder
**TravelMediaRequest** — description, width, height, duration, takenAt

**TravelResponse** — 전체 Travel 정보 + members(List<TravelMemberResponse>) + memberCount
**TravelListResponse** — travels(List<TravelResponse>) + pagination(totalCount, pageSize, currentPage, hasMore)

**TravelChannelCreateRequest** — name 필수(2-50자), description(max 200), contextType, contextId, channelPurpose, isPinned, displayOrder
**TravelChannelUpdateRequest** — contextType, contextId, channelPurpose, isPinned, displayOrder (전부 optional)
**TravelChannelResponse** — bridge 필드(id, travelId, channelId, contextType, contextId, channelPurpose, isPinned, displayOrder) + channel 정보(channelName, channelDescription, memberCount, lastMessageAt) + createdAt, updatedAt

### 11.5 Mapper
`TravelMapper` (@Component) — Entity ↔ Response 변환. 생성 시 visibility 미입력이면 PRIVATE, status는 PLANNING 기본값.
`TravelChannelMapper` (@Component) — TravelChannel ↔ Response 변환 + ChannelCreateRequest 생성. contextType 미입력이면 GENERAL 기본값. channelType은 GROUP 고정.

### 11.6 Controller 엔드포인트

**TravelController** (`/api/v1/travels`)
```
# 여행 CRUD
POST   /api/v1/travels                                    → 생성
GET    /api/v1/travels/{travelId}                         → 상세 조회
PUT    /api/v1/travels/{travelId}                         → 수정
DELETE /api/v1/travels/{travelId}                         → 삭제 (204)

# 목록
GET    /api/v1/travels/my?page=0&size=10                  → 내 여행 목록
GET    /api/v1/travels/public?page=0&size=10              → 공개 여행 목록

# 멤버
POST   /api/v1/travels/{travelId}/members                 → 멤버 추가
DELETE /api/v1/travels/{travelId}/members/{userId}        → 멤버 제거 (204)
PUT    /api/v1/travels/{travelId}/members/{userId}/role?role=ADMIN → 역할 변경

# 일정
POST   /api/v1/travels/{travelId}/schedules               → 일정 추가
PUT    /api/v1/travels/{travelId}/schedules/{scheduleId}  → 일정 수정
DELETE /api/v1/travels/{travelId}/schedules/{scheduleId}  → 일정 삭제 (204)

# 미디어
POST   /api/v1/travels/{travelId}/media                   → 업로드 (multipart/form-data)
GET    /api/v1/travels/{travelId}/media?page=0&size=20    → 목록 조회
DELETE /api/v1/travels/{travelId}/media/{mediaId}         → 삭제 (204)
```

**TravelChannelController** (`/api/v1/travels/{travelId}/channels`)
```
POST   /api/v1/travels/{travelId}/channels                          → 채널 생성
GET    /api/v1/travels/{travelId}/channels                          → 채널 목록 조회
GET    /api/v1/travels/{travelId}/channels/{travelChannelId}        → 단일 채널 조회
PUT    /api/v1/travels/{travelId}/channels/{travelChannelId}        → 메타데이터 수정
DELETE /api/v1/travels/{travelId}/channels/{travelChannelId}        → 삭제 (204)
POST   /api/v1/travels/{travelId}/channels/{travelChannelId}/sync   → 멤버 동기화
```

### 11.7 TravelChannel 브릿지 모델

Travel 모듈과 Chat 모듈은 **TravelChannel 브릿지**로 느슨하게 연결된다.
기존 Chat 모듈(Channels, Messages, ChannelMembers)은 수정하지 않는다.

```
Travels 1:N TravelChannel 1:1 Channels
```

**핵심 동작:**
- **채널 생성**: TravelChannelService → ChannelService.createChannel() 호출 → bridge 저장 → Travels.channelIds 동기화
- **채널 목록**: bridge 조회 → ChannelsRepo.findByIdIn()으로 채널 정보 일괄 조회 (N+1 방지)
- **채널 삭제**: bridge soft delete → Travels.channelIds에서 제거 → ChannelService.archiveChannel()
- **멤버 동기화**: TravelUsers → ChannelMemberService.addMember() (이미 멤버면 skip)

**contextType + contextId 조합:**
| contextType | contextId | 용도 |
|-------------|-----------|------|
| GENERAL | null | 자유 토론방 |
| ITINERARY | itineraryId | 특정 일정 토론 |
| PLACE | placeId | 특정 장소 토론 |
| MEDIA | null | 사진 공유 |
| INFO | null | 교통편/숙소 정보 |

**의존성 (TravelChannelServiceImpl):**
- `TravelChannelRepo`, `TravelsRepo`, `TravelUsersRepo` (travel 모듈)
- `ChannelService`, `ChannelMemberService`, `ChannelsRepo` (chat 모듈)
- `TravelChannelMapper` (travel 모듈)

**주의사항:**
1. Chat 모듈 무수정 원칙
2. TravelChannel은 travel 모듈 소속
3. channelId는 유니크 — 하나의 채널은 하나의 Travel에만 소속
4. 채널 삭제 = bridge soft delete + 채널 archive (메시지 데이터 보존)
5. 권한 체크: CRUD는 해당 Travel의 멤버(ADMIN)만 가능, 조회는 멤버 전체
6. deleteTravel 시 travelChannelRepo.deleteByTravelId()로 일괄 정리

### 11.8 참고 사항
- 미디어 업로드는 `multipart/form-data` 형식: `file` (필수) + `request` (선택, JSON 메타데이터)
- S3Service를 통해 파일 업로드 처리
- 여행 생성자는 자동으로 ADMIN 역할 멤버로 등록됨
- TravelSchedule은 Travels 문서 내 내장 배열 (별도 컬렉션이 아님)