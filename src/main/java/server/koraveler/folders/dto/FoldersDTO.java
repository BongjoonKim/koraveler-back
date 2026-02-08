package server.koraveler.folders.dto;

import lombok.Data;
import server.koraveler.folders.model.Folders;

@Data
public class FoldersDTO extends Folders {
    private FoldersDTO parentFolder;
}
