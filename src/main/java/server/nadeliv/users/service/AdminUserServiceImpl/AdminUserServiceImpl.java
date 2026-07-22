// AdminUserServiceImpl.java
package server.nadeliv.users.service.AdminUserServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import server.nadeliv.blog.model.Documents;
import server.nadeliv.blog.repo.BlogsRepo;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
import server.nadeliv.travel.model.entities.TravelUsers;
import server.nadeliv.travel.model.entities.Travels;
import server.nadeliv.travel.repo.TravelUsersRepo;
import server.nadeliv.travel.repo.TravelsRepo;
import server.nadeliv.users.dto.request.AdminRoleUpdateRequest;
import server.nadeliv.users.dto.request.AdminStatusUpdateRequest;
import server.nadeliv.users.dto.response.AdminUserDetailResponse;
import server.nadeliv.users.dto.response.AdminUserListResponse;
import server.nadeliv.users.dto.response.AdminUserSummaryResponse;
import server.nadeliv.users.model.Users;
import server.nadeliv.users.repo.UsersRepo;
import server.nadeliv.users.service.AdminUserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    // 부여 가능한 권한 화이트리스트
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "admin");

    private final UsersRepo usersRepo;
    private final BlogsRepo blogsRepo;
    private final TravelUsersRepo travelUsersRepo;
    private final TravelsRepo travelsRepo;
    private final MongoTemplate mongoTemplate;

    @Override
    public AdminUserListResponse getUsers(int page, int size, String keyword, String status) {
        // 검색어(userId·name·email OR)와 상태 필터를 조합해야 하므로
        // 파생 쿼리의 Or/And 우선순위 함정을 피해 Criteria 로 직접 조립한다.
        List<Criteria> conditions = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String quoted = Pattern.quote(keyword.trim());
            conditions.add(new Criteria().orOperator(
                    Criteria.where("userId").regex(quoted, "i"),
                    Criteria.where("name").regex(quoted, "i"),
                    Criteria.where("email").regex(quoted, "i")
            ));
        }
        if ("active".equalsIgnoreCase(status)) {
            conditions.add(Criteria.where("isEnabled").is(true));
        } else if ("disabled".equalsIgnoreCase(status)) {
            conditions.add(Criteria.where("isEnabled").is(false));
        }

        Query query = new Query();
        if (!conditions.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(conditions.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Users.class);
        int totalPages = (int) Math.ceil((double) total / size);

        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created")));
        List<Users> users = mongoTemplate.find(query, Users.class);

        long totalAll = usersRepo.count();
        long activeCount = usersRepo.countByIsEnabledTrue();

        return AdminUserListResponse.builder()
                .users(users.stream().map(this::toSummary).collect(Collectors.toList()))
                .totalCount(total)
                .totalPages(totalPages)
                .currentPage(page)
                .hasNext(page + 1 < totalPages)
                .totalAll(totalAll)
                .activeCount(activeCount)
                .disabledCount(totalAll - activeCount)
                .build();
    }

    @Override
    public AdminUserDetailResponse getUserDetail(String userId, int docPage, int docSize) {
        Users user = usersRepo.findByUserId(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 작성한 글 (draft·휴지통 포함, 최신순)
        Pageable docPageable = PageRequest.of(docPage, docSize, Sort.by(Sort.Direction.DESC, "created"));
        Page<Documents> docs = blogsRepo.findByCreatedUser(userId, docPageable);

        // 참여 중인 여행 프로젝트 (N+1 방지: findByIdIn 일괄 조회)
        List<TravelUsers> memberships = travelUsersRepo.findByUserId(userId);
        Map<String, TravelUsers> membershipByTravelId = memberships.stream()
                .collect(Collectors.toMap(TravelUsers::getTravelId, Function.identity(), (a, b) -> a));
        List<Travels> travels = membershipByTravelId.isEmpty()
                ? List.of()
                : travelsRepo.findByIdIn(new ArrayList<>(membershipByTravelId.keySet()));

        List<AdminUserDetailResponse.TravelSummary> travelSummaries = travels.stream()
                .map(travel -> {
                    TravelUsers membership = membershipByTravelId.get(travel.getId());
                    return AdminUserDetailResponse.TravelSummary.builder()
                            .id(travel.getId())
                            .title(travel.getTitle())
                            .destination(travel.getDestination())
                            .status(travel.getStatus() != null ? travel.getStatus().name() : null)
                            .visibility(travel.getVisibility() != null ? travel.getVisibility().name() : null)
                            .startDate(travel.getStartDate())
                            .endDate(travel.getEndDate())
                            .role(membership != null && membership.getRole() != null
                                    ? membership.getRole().name() : null)
                            .memberCount(travelUsersRepo.countByTravelId(travel.getId()))
                            .created(travel.getCreated())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminUserDetailResponse.builder()
                .user(toSummary(user))
                .documentCount(docs.getTotalElements())
                .documents(docs.getContent().stream()
                        .map(doc -> AdminUserDetailResponse.DocumentSummary.builder()
                                .id(doc.getId())
                                .title(doc.getTitle())
                                .draft(doc.isDraft())
                                .disclose(doc.isDisclose())
                                .deleted(doc.isDeleted())
                                .thumbnailImgUrl(doc.getThumbnailImgUrl())
                                .tags(doc.getTags())
                                .created(doc.getCreated())
                                .updated(doc.getUpdated())
                                .build())
                        .collect(Collectors.toList()))
                .documentPage(docPage)
                .documentTotalPages(docs.getTotalPages())
                .travelCount((long) travelSummaries.size())
                .travels(travelSummaries)
                .build();
    }

    @Override
    public AdminUserSummaryResponse updateRoles(String targetUserId, AdminRoleUpdateRequest request, String adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            // 마지막 관리자가 스스로 admin 을 내리는 락아웃 방지
            throw new CustomException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }

        List<String> roles = request.getRoles();
        if (roles == null || roles.isEmpty() || !ALLOWED_ROLES.containsAll(roles)) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }
        // user 는 기본 권한으로 항상 유지
        List<String> normalized = new ArrayList<>();
        normalized.add("user");
        if (roles.contains("admin")) {
            normalized.add("admin");
        }

        Users user = usersRepo.findByUserId(targetUserId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        user.setRoles(normalized);
        user.setAuthorities(new ArrayList<>(normalized));
        user.setUpdated(LocalDateTime.now());
        Users saved = usersRepo.save(user);

        log.info("[admin] roles of {} changed to {} by {}", targetUserId, normalized, adminUserId);
        return toSummary(saved);
    }

    @Override
    public AdminUserSummaryResponse updateStatus(String targetUserId, AdminStatusUpdateRequest request, String adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new CustomException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF);
        }
        if (request.getEnabled() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Users user = usersRepo.findByUserId(targetUserId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 회원 탈퇴와 동일한 soft-disable 방식 (UsersServiceImpl.deleteUser 와 동일 semantics)
        user.setEnabled(request.getEnabled());
        user.setUpdated(LocalDateTime.now());
        Users saved = usersRepo.save(user);

        log.info("[admin] user {} {} by {}", targetUserId,
                request.getEnabled() ? "re-enabled" : "disabled", adminUserId);
        return toSummary(saved);
    }

    private AdminUserSummaryResponse toSummary(Users user) {
        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .src(user.getSrc())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .created(user.getCreated())
                .updated(user.getUpdated())
                .build();
    }
}
