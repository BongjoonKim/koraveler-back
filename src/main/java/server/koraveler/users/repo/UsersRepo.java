// UsersRepo.java
package server.koraveler.users.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.users.model.Users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepo extends MongoRepository<Users, String> {

    // ===== 기존 메서드들 =====
    Users findByEmail(String email);
    Users findByUserId(String userId);

    // ===== 추가 기본 조회 메서드들 =====

    /**
     * 사용자 ID로 존재 여부 확인
     */
    boolean existsByUserId(String userId);

    /**
     * 이메일로 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 ID 목록으로 조회
     */
    List<Users> findByUserIdIn(List<String> userIds);

    /**
     * 활성 사용자만 조회
     */
    List<Users> findByIsEnabledTrue();

    /**
     * 활성 사용자 페이징 조회
     */
    Page<Users> findByIsEnabledTrue(Pageable pageable);

    /**
     * 비활성 사용자 조회
     */
    List<Users> findByIsEnabledFalse();

    // ===== 검색 관련 메서드들 (Spring Data MongoDB 네이밍 컨벤션) =====

    /**
     * userId로 부분 검색 (대소문자 무시)
     */
    List<Users> findByUserIdContainingIgnoreCase(String userId);

    /**
     * userId로 부분 검색 + 페이징 (대소문자 무시)
     */
    Page<Users> findByUserIdContainingIgnoreCase(String userId, Pageable pageable);

    /**
     * 이름으로 부분 검색 (대소문자 무시)
     */
    List<Users> findByNameContainingIgnoreCase(String name);

    /**
     * 이름으로 부분 검색 + 페이징 (대소문자 무시)
     */
    Page<Users> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 이메일로 부분 검색 (대소문자 무시)
     */
    List<Users> findByEmailContainingIgnoreCase(String email);

    /**
     * 이메일로 부분 검색 + 페이징 (대소문자 무시)
     */
    Page<Users> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * userId 또는 이름으로 검색 (대소문자 무시)
     */
    List<Users> findByUserIdContainingIgnoreCaseOrNameContainingIgnoreCase(
            String userId, String name);

    /**
     * userId 또는 이름으로 검색 + 페이징 (대소문자 무시)
     */
    Page<Users> findByUserIdContainingIgnoreCaseOrNameContainingIgnoreCase(
            String userId, String name, Pageable pageable);

    /**
     * userId, 이름, 이메일로 검색 (대소문자 무시)
     */
    List<Users> findByUserIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String userId, String name, String email);

    /**
     * userId, 이름, 이메일로 검색 + 페이징 (대소문자 무시)
     */
    Page<Users> findByUserIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String userId, String name, String email, Pageable pageable);

    /**
     * 활성 사용자 중에서 userId로 검색
     */
    List<Users> findByIsEnabledTrueAndUserIdContainingIgnoreCase(String userId);

    /**
     * 활성 사용자 중에서 이름으로 검색
     */
    List<Users> findByIsEnabledTrueAndNameContainingIgnoreCase(String name);

    /**
     * 활성 사용자 중에서 userId, 이름, 이메일로 검색
     */
    List<Users> findByIsEnabledTrueAndUserIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String userId, String name, String email);

    /**
     * 활성 사용자 중에서 키워드 검색 + 페이징
     */
    Page<Users> findByIsEnabledTrueAndUserIdContainingIgnoreCaseOrIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndEmailContainingIgnoreCase(
            String userId, String name, String email, Pageable pageable);

    /**
     * 특정 사용자들을 제외하고 조회
     */
    List<Users> findByUserIdNotIn(List<String> excludeUserIds);

    /**
     * 특정 사용자들을 제외하고 활성 사용자 조회
     */
    List<Users> findByIsEnabledTrueAndUserIdNotIn(List<String> excludeUserIds);

    /**
     * 특정 사용자들을 제외하고 활성 사용자 조회 + 페이징
     */
    Page<Users> findByIsEnabledTrueAndUserIdNotIn(List<String> excludeUserIds, Pageable pageable);

    /**
     * 특정 사용자를 제외하고 userId로 검색
     */
    List<Users> findByUserIdContainingIgnoreCaseAndUserIdNotIn(
            String keyword, List<String> excludeUserIds);

    /**
     * 특정 사용자를 제외하고 활성 사용자 중 검색
     */
    List<Users> findByIsEnabledTrueAndUserIdContainingIgnoreCaseAndUserIdNotIn(
            String keyword, List<String> excludeUserIds);

    // ===== 정렬 관련 메서드들 =====

    /**
     * 최근 가입한 사용자 조회 (상위 10명)
     */
    List<Users> findTop10ByIsEnabledTrueOrderByCreatedDesc();

    /**
     * 최근 가입한 사용자 조회 (지정된 수만큼)
     */
    List<Users> findTopNByIsEnabledTrueOrderByCreatedDesc(int n);

    /**
     * 이름순으로 정렬하여 조회
     */
    List<Users> findByIsEnabledTrueOrderByNameAsc();

    /**
     * 이름순으로 정렬하여 페이징 조회
     */
    Page<Users> findByIsEnabledTrueOrderByNameAsc(Pageable pageable);

    // ===== 날짜 관련 메서드들 =====

    /**
     * 특정 날짜 이후 가입한 사용자 조회
     */
    List<Users> findByCreatedAfter(LocalDateTime date);

    /**
     * 특정 날짜 이전 가입한 사용자 조회
     */
    List<Users> findByCreatedBefore(LocalDateTime date);

    /**
     * 특정 기간 내 가입한 사용자 조회
     */
    List<Users> findByCreatedBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 기간 내 가입한 활성 사용자 조회
     */
    List<Users> findByIsEnabledTrueAndCreatedBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 기간 내 가입한 사용자 수 카운트
     */
    Long countByCreatedBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 활성 사용자 수 카운트
     */
    Long countByIsEnabledTrue();

    // ===== 정확한 일치 검색 =====

    /**
     * userId 대소문자 무시하고 정확히 일치
     */
    Optional<Users> findByUserIdIgnoreCase(String userId);

    /**
     * 이메일 대소문자 무시하고 정확히 일치
     */
    Optional<Users> findByEmailIgnoreCase(String email);

    /**
     * 이름으로 정확히 일치하는 사용자 조회
     */
    List<Users> findByName(String name);

    /**
     * 이름 대소문자 무시하고 정확히 일치
     */
    List<Users> findByNameIgnoreCase(String name);
}