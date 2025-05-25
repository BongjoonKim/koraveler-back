package server.koraveler.folders.service.folderServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import server.koraveler.blog.model.Documents;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.folders.dto.FoldersDTO;
import server.koraveler.folders.model.Folders;
import server.koraveler.folders.repo.FoldersRepo;
import server.koraveler.folders.service.FolderService;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FolderServiceImpl implements FolderService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private FoldersRepo foldersRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private BlogsRepo blogsRepo;

    @Override
    public FoldersDTO createFolder(FoldersDTO foldersDTO) {
        Folders folders = new Folders();
        BeanUtils.copyProperties(foldersDTO, folders);
        LocalDateTime now = LocalDateTime.now();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Users users = usersRepo.findByUserId(username);

            if (!ObjectUtils.isEmpty(users)) {
                folders.setCreatedUser(username);
                folders.setCreated(now);
                folders.setUpdatedUser(username);
                folders.setUpdated(now);
                folders.setUserId(username);

                if (folders.getParentId() == null) {
                    folders.setParentId(users.getId());
                    folders.setPath("/" + users.getId());
                }
                Folders result = foldersRepo.save(folders);
                FoldersDTO newFoldersDTO = new FoldersDTO();
                BeanUtils.copyProperties(result, newFoldersDTO);
                return newFoldersDTO;
            }
        }
        return null;
    }

    @Override
    public FoldersDTO saveFolder(FoldersDTO foldersDTO) {
        Folders folders = new Folders();
        BeanUtils.copyProperties(foldersDTO, folders);
        LocalDateTime now = LocalDateTime.now();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Users users = usersRepo.findByUserId(username);

            if (!ObjectUtils.isEmpty(users)) {
                folders.setUpdatedUser(username);
                folders.setUpdated(now);
                folders.setUserId(username);

                if (folders.getParentId() == null) {
                    folders.setParentId(users.getId());
                    folders.setPath("/" + users.getId());
                }
                Folders result = foldersRepo.save(folders);
                FoldersDTO newFoldersDTO = new FoldersDTO();
                BeanUtils.copyProperties(result, newFoldersDTO);
                return newFoldersDTO;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getAllLoginUserFolders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Users users = usersRepo.findByUserId(username);

            List<Folders> allFolders = foldersRepo.findByPathRegex("^/" + users.getId() + "*");

            return buildFolderTree(allFolders, users.getId());
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> buildFolderTree(List<Folders> folders, String userId) {
        Map<String, Object> result = new HashMap<>();

        // 루트 폴더 생성 (가상 폴더)
        Map<String, Object> rootFolder = new HashMap<>();
        rootFolder.put("index", "root");
        rootFolder.put("isFolder", true);
        rootFolder.put("data", "Root");
        rootFolder.put("children", new ArrayList<>());
        result.put("root", rootFolder);

        // 폴더별 자식 관계 매핑을 위한 Map
        Map<String, List<String>> parentChildMap = new HashMap<>();

        // 각 폴더를 result에 추가하고 부모-자식 관계 매핑
        for (Folders folder : folders) {
            // 폴더 데이터 생성
            Map<String, Object> folderData = new HashMap<>();
            folderData.put("index", folder.getId());
            folderData.put("isFolder", true);
            folderData.put("data", folder);
            folderData.put("children", new ArrayList<>());

            // 추가 메타데이터 (필요시)
            folderData.put("description", folder.getDescription());
            folderData.put("created", folder.getCreated());
            folderData.put("updated", folder.getUpdated());

            result.put(folder.getId(), folderData);

            // 부모-자식 관계 매핑
            String parentKey = determineParentKey(folder, userId);
            parentChildMap.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(folder.getId());
        }

        // 자식 관계 설정
        for (Map.Entry<String, List<String>> entry : parentChildMap.entrySet()) {
            String parentKey = entry.getKey();
            List<String> children = entry.getValue();

            if (result.containsKey(parentKey)) {
                ((List<String>) ((Map<String, Object>) result.get(parentKey)).get("children")).addAll(children);
            }
        }

        return result;
    }

    private String determineParentKey(Folders folder, String userId) {
        String path = folder.getPath();

        // path가 "/" + userId 형태면 루트의 직접 자식
        if (path.equals("/" + userId)) {
            return "root";
        }

        // path에서 마지막 "/" 이후의 ID를 부모 ID로 사용
        String[] pathParts = path.split("/");
        if (pathParts.length >= 2) {
            // 마지막 부분이 부모 ID
            return pathParts[pathParts.length - 1];
        }

        // 기본적으로 루트의 자식으로 처리
        return "root";
    }

    @Override
    public void deleteFolder(String id) {
        // 삭제할 폴더 조회
        Folders folderToDelete = foldersRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("폴더를 찾을 수 없습니다: " + id));

        // 최상위 폴더(userId와 같은 이름의 폴더)인지 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Users users = usersRepo.findByUserId(username);
            if (users.getId().equals(folderToDelete.getUserId())) {
                throw new RuntimeException("최상위 폴더는 삭제할 수 없습니다.");
            }
        } else {
            throw new RuntimeException("로그인이 필요합니다.");
        }


        String parentId = folderToDelete.getParentId();
        String userId = folderToDelete.getUserId();
        String folderPath = folderToDelete.getPath();

        // 경로에 삭제할 폴더의 path가 포함된 모든 하위 폴더 찾기
        // 정규식을 사용하여 폴더의 path로 시작하는 모든 경로를 찾음
        List<Folders> descendantFolders = foldersRepo.findByPathRegex("^" + folderPath + "/.*");

        // 직접적인 자식 폴더들(parentId가 삭제할 폴더 ID인 폴더들)
        List<Folders> childFolders = foldersRepo.findByParentId(id);

        // 직접적인 자식 폴더들의 부모 ID를 삭제할 폴더의 부모 ID로 변경
        for (Folders childFolder : childFolders) {
            childFolder.setParentId(parentId);
            foldersRepo.save(childFolder);
        }

        // 모든 하위 폴더의 경로 업데이트
        for (Folders descendantFolder : descendantFolders) {
            // 경로에서 삭제할 폴더의 path 부분을 삭제할 폴더의 부모 경로로 대체
            // 예: "parent/delete/child" -> "parent/child"
            String oldPath = descendantFolder.getPath();

            // folderPath에서 parentPath를 추출 (마지막 '/'를 찾아 그 앞까지의 문자열)
            String parentPath = "";
            int lastSlashIndex = folderPath.lastIndexOf('/');
            if (lastSlashIndex > 0) {
                parentPath = folderPath.substring(0, lastSlashIndex);
            }

            // 새 경로 생성: 삭제할 폴더 부분을 제거하고 부모 경로와 나머지 부분을 연결
            String newPath = oldPath.replace(folderPath, parentPath);

            descendantFolder.setPath(newPath);
            foldersRepo.save(descendantFolder);
        }

        // 해당 폴더에 있는 문서들의 폴더 ID 업데이트

        List<Documents> documents = blogsRepo.findAll().stream()
                .filter(doc -> doc.getFolderId() != null && doc.getFolderId().equals(id))
                .toList();

        // 최상위 폴더 ID 조회 (userId와 동일)
        String rootFolderId = foldersRepo.findByUserIdAndParentId(userId, null)
                .stream()
                .filter(folder -> folder.getName().equals(userId))
                .findFirst()
                .map(Folders::getId)
                .orElse(userId); // 최상위 폴더가 없는 경우 userId를 기본값으로 사용

        // 문서들의 폴더 ID를 최상위 폴더 ID로 변경
        for (Documents document : documents) {
            document.setFolderId(rootFolderId);
            blogsRepo.save(document);
        }

        // 최종적으로 폴더 삭제
        foldersRepo.deleteById(id);
    }
}
