package server.koraveler.connections.bookmarks.service.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import server.koraveler.blog.dto.DocumentsDTO;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.model.Documents;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.connections.bookmarks.dto.BookmarkDTO;
import server.koraveler.connections.bookmarks.model.Bookmark;
import server.koraveler.connections.bookmarks.repo.BookmarksRepo;
import server.koraveler.connections.bookmarks.service.ConBookmarksUsersDocumentsService;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ConBookmarksUsersDocumentsServiceImpl implements ConBookmarksUsersDocumentsService {

    @Autowired
    private BookmarksRepo bookmarksRepo;

    @Autowired
    private BlogsRepo blogsRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Override
    public BookmarkDTO createBookmark(BookmarkDTO bookmarkDTO) throws Exception {
        // 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Users users = usersRepo.findByUserId(username);
            if (!ObjectUtils.isEmpty(users)) {
                LocalDateTime now = LocalDateTime.now();
                Bookmark bookmark = new Bookmark();
                bookmark.setDocumentId(bookmarkDTO.getDocumentId());
                bookmark.setUserId(users.getUserId());
                bookmark.setBookmarked(true);
                bookmark.setCreated(now);
                bookmark.setUpdated(now);
                bookmark.setCreatedUser(users.getUserId());
                bookmark.setUpdatedUser(users.getUserId());

                Bookmark newBookmark = bookmarksRepo.insert(bookmark);
                BookmarkDTO newBookmarkDTO = new BookmarkDTO();
                BeanUtils.copyProperties(newBookmark, newBookmarkDTO);

                return newBookmarkDTO;
            } else {
                throw new Exception("there is no document");
            }
        }

        // 글 정보 가져오기

        // 실제 글이 존재한다면 북마크 저장
        return null;
    }

    @Override
    public boolean isBookmarked(String documentId) throws Exception {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();
                System.out.println("username = " + username);
                Users users = usersRepo.findByUserId(username);
                if (!ObjectUtils.isEmpty(users)) {
                    Documents document = blogsRepo.findById(documentId).get();
                    if (!ObjectUtils.isEmpty(document)) {
                        Bookmark bookmark = bookmarksRepo.findByDocumentIdAndUserId(document.getId(), users.getUserId());
                        if (ObjectUtils.isEmpty(bookmark)) {
                            return false;   // 북마크 안 되어있음
                        } else {
                            return true;    // 북마크 되어있음
                        }
                    } else {
                        throw new Exception("there is no document");
                    }
                } else {
                    throw new Exception("there is no user : " + username);
                }
            }
            return false;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public List<DocumentsDTO> getDocuments() throws Exception {
        return null;
    }

    @Override
    public void deleteBookmarkByDocumentsId(String documentId) throws Exception {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();
                System.out.println("username = " + username);
                Users users = usersRepo.findByUserId(username);
                if (!ObjectUtils.isEmpty(users)) {
                    Documents document = blogsRepo.findById(documentId).get();
                    if (!ObjectUtils.isEmpty(document)) {
                        Bookmark bookmark = bookmarksRepo.findByDocumentIdAndUserId(document.getId(), users.getUserId());
                        if (ObjectUtils.isEmpty(bookmark)) {
                            // 북마크로 애초에 선택이 안 되어있었기 때문에 삭제 안 함
                        } else {
                            String bookmarkId = bookmark.getId();
                            bookmarksRepo.deleteById(bookmarkId);
                        }
                    } else {
                        throw new Exception("there is no document");
                    }
                } else {
                    throw new Exception("there is no user : " + username);
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void deleteBookmarkByUsers(String userId) throws Exception {

    }
}
