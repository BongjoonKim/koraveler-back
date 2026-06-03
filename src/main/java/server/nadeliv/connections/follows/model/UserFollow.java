package server.nadeliv.connections.follows.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.common.dto.CommonDTO;

/**
 * "follower 가 following 을 follow 한다" 관계.
 * followerId/followingId 모두 Users.userId (string identifier, 비-_id) 를 저장.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "con_user_follows")
@CompoundIndexes({
        @CompoundIndex(name = "follower_following_unique_idx",
                def = "{'followerId': 1, 'followingId': 1}", unique = true)
})
public class UserFollow extends CommonDTO {
    @Id
    private String id;

    @Indexed
    private String followerId;   // 팔로우 하는 사용자 (Users.userId)

    @Indexed
    private String followingId;  // 팔로우 받는 사용자 (Users.userId)
}
