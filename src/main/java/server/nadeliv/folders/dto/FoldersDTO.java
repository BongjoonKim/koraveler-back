package server.nadeliv.folders.dto;

import lombok.Data;
import server.nadeliv.folders.model.Folders;

@Data
public class FoldersDTO extends Folders {
    private FoldersDTO parentFolder;
}
