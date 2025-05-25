package server.koraveler.folders.service;

import server.koraveler.folders.dto.FoldersDTO;

import java.util.List;
import java.util.Map;

public interface FolderService {
    FoldersDTO createFolder(FoldersDTO foldersDTO);
    FoldersDTO saveFolder(FoldersDTO foldersDTO);
    Map<String, Object> getAllLoginUserFolders();
    void deleteFolder(String id);
}
