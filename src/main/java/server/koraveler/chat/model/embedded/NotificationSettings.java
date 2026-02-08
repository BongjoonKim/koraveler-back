package server.koraveler.chat.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationSettings {
    private Boolean pushEnabled;
    private Boolean emailEnabled;
    private Boolean soundEnabled;
    private String soundType;
    private List<String> mutedKeywords;
}