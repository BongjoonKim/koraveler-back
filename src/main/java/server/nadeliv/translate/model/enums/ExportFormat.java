// ExportFormat.java
package server.nadeliv.translate.model.enums;

public enum ExportFormat {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    TXT("txt", "text/plain");

    private final String extension;
    private final String mimeType;

    ExportFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }
}