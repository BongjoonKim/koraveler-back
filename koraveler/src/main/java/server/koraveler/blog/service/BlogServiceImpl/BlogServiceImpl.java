package server.koraveler.blog.service.BlogServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.model.Documents;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.blog.service.BlogService;

import java.time.LocalDateTime;

@Service
public class BlogServiceImpl implements BlogService {
    @Autowired
    private BlogsRepo blogsRepo;

    @Override
    public DocumentsInfo.DocumentsDTO createDocument(DocumentsInfo.DocumentsDTO documentsDTO) {

        Documents documents = new Documents();
        BeanUtils.copyProperties(documentsDTO, documents);
        LocalDateTime now = LocalDateTime.now();

        documents.setCreated(now);
        documents.setUpdated(now);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            System.out.println("userDetails = " + userDetails);
            String username = userDetails.getUsername();
            documents.setCreatedUser(username);
            documents.setUpdatedUser(username);
            Documents afterDocument = blogsRepo.save(documents);

            DocumentsInfo.DocumentsDTO newDocDTO = new DocumentsInfo.DocumentsDTO();
            BeanUtils.copyProperties(afterDocument, newDocDTO);

            return newDocDTO;
        }


        return null;
    }
}
