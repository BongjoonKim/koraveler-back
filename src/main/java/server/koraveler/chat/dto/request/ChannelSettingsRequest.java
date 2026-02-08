// 9. ChannelSettingsRequest.java
package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.embedded.NotificationSettings;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelSettingsRequest {
    private NotificationSettings defaultNotificationSettings;
    private Boolean requiresApproval;
    private String password;
    private Integer slowModeSeconds;
    private Boolean allowGuestAccess;
}