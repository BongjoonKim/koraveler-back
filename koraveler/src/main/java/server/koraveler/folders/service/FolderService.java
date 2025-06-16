package server.koraveler.folders.service;

import server.koraveler.folders.dto.FoldersDTO;

import java.util.List;
import java.util.Map;

public interface FolderService {
    FoldersDTO saveFolder(FoldersDTO foldersDTO) throws Exception;
    Map<String, Object>  getAllLoginUserFolders();
    void deleteFolder(String id);
    FoldersDTO getParentFolder(String childId) throws Exception;
}
