// 4. ChannelListResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelListResponse {
    private List<ChannelResponse> channels;
    private Integer totalCount;
    private Boolean hasNext;
    private String nextCursor;
}