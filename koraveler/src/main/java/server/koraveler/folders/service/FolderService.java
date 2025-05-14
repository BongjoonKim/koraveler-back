package server.koraveler.folders.service;

import server.koraveler.folders.dto.FoldersDTO;

import java.util.List;

public interface FolderService {
    FoldersDTO createFolder(FoldersDTO foldersDTO);
    FoldersDTO saveFolder(FoldersDTO foldersDTO);
    List<FoldersDTO> getAllLoginUserFolders();
    void deleteFolder(String id);
}
