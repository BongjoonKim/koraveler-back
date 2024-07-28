package server.koraveler.blog.service.BlogServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.blog.service.BlogService;

@Service
public class BlogServiceImpl implements BlogService {
    @Autowired
    private BlogsRepo blogsRepo;

    @Override
    public DocumentsInfo.DocumentsDTO createDocument(DocumentsInfo.DocumentsDTO documentsDTO) {
        return null;
    }
}
