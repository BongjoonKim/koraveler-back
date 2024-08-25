package server.koraveler.blog.service.BlogServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.model.Documents;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.blog.service.BlogService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public DocumentsInfo.DocumentsDTO saveDocument(DocumentsInfo.DocumentsDTO documentsDTO) {
        try {
            Documents documents = new Documents();
            BeanUtils.copyProperties(documentsDTO, documents);
            LocalDateTime now = LocalDateTime.now();

            documents.setUpdated(now);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                System.out.println("userDetails = " + userDetails);
                String username = userDetails.getUsername();
                documents.setUpdatedUser(username);
                Documents afterDocument = blogsRepo.save(documents);

                DocumentsInfo.DocumentsDTO newDocDTO = new DocumentsInfo.DocumentsDTO();
                BeanUtils.copyProperties(afterDocument, newDocDTO);

                return newDocDTO;
            }
            return null;
        } catch (Exception e) {
            throw e;
        }
    }


    @Override
    public DocumentsInfo getAllDocuments(PaginationDTO pageDTO) {
        try {
            Page<Documents> documents = null;
            if ("all".equals(pageDTO.getFolderId()) || pageDTO.getPage() == -1) {
                documents = blogsRepo.findAll(PageRequest.of(pageDTO.getPage(), pageDTO.getSize()));
            } else {
                documents = blogsRepo.findAll(PageRequest.of(pageDTO.getPage(), pageDTO.getSize()));
            }
            List<DocumentsInfo.DocumentsDTO> documentsDTO = documents.getContent().stream().map(document -> {
                DocumentsInfo.DocumentsDTO documentDTO = new DocumentsInfo.DocumentsDTO();
                BeanUtils.copyProperties(document, documentDTO);
                return documentDTO;
            }).collect(Collectors.toList());

            DocumentsInfo documentsInfo = new DocumentsInfo();
            documentsInfo.setDocumentsDTO(documentsDTO);
            documentsInfo.setTotalPagesCnt(documents.getTotalPages());
            documentsInfo.setTotalDocsCnt(documents.getTotalElements());
            return documentsInfo;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public DocumentsInfo.DocumentsDTO createAfterSaveDocument(DocumentsInfo.DocumentsDTO newData) {
        LocalDateTime now = LocalDateTime.now();
        Documents documents = blogsRepo.findById(newData.getId()).get();

        documents.setContents(newData.getContents());
        documents.setThumbnailImgUrl(newData.getThumbnailImgUrl());

        Documents newDocument = blogsRepo.save(documents);
        DocumentsInfo.DocumentsDTO newDocumentDTO = new DocumentsInfo.DocumentsDTO();
        BeanUtils.copyProperties(newDocument, newDocumentDTO);

        return newDocumentDTO;
    }

    @Override
    public DocumentsInfo.DocumentsDTO getDocument(String id) throws Exception {
        try {
            Documents documents = blogsRepo.findById(id).get();
            DocumentsInfo.DocumentsDTO documentsDTO = new DocumentsInfo.DocumentsDTO();
            BeanUtils.copyProperties(documents, documentsDTO);
            return documentsDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void deleteDocument(String id) throws Exception {
        try {
            blogsRepo.deleteById(id);
        } catch (Exception e) {
            throw e;
        }
    }
}
