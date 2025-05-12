package server.koraveler.folders.service;

import server.koraveler.folders.dto.FoldersDTO;

public interface FolderService {
    FoldersDTO createFolder(FoldersDTO foldersDTO);
    FoldersDTO saveFolder(FoldersDTO foldersDTO);
    FoldersDTO getFolder(String id);
    void deleteFolder(String id);
}
