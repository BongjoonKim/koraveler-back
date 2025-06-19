package server.koraveler.chat.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.chat.model.embedded.NotificationSettings;
import server.koraveler.chat.model.enums.ChannelType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "channels")
public class Channels {
    @Id
    private String id;
    private String name;
    private String description;
    private String createdUserId;
    private String updatedUserId;
    private ChannelType channelType;
    private Boolean isArchived;
    private Boolean isReadOnly;
    private String password;
    private Boolean requiresApproval;
    private Integer maxMembers;
    private String avatarUrl;
    private String topic;
    private List<String> tags;
    private Integer memberCount;
    private LocalDateTime lastMessageAt;
    private String lastMessageId;
    private NotificationSettings defaultNotificationSettings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;
}