// 11. ReactionMapper.java (Helper for MessageMapper)
package server.koraveler.chat.dto.mapper;

import server.koraveler.chat.dto.response.ReactionResponse;
import server.koraveler.chat.model.embedded.MessageReaction;

import java.util.List;
import java.util.stream.Collectors;

public class ReactionMapper {

    public static List<ReactionResponse> toResponseList(List<MessageReaction> reactions) {
        if (reactions == null) return List.of();

        return reactions.stream()
                .map(ReactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public static ReactionResponse toResponse(MessageReaction reaction) {
        return ReactionResponse.builder()
                .emoji(reaction.getEmoji())
                .count(reaction.getCount())
                .users(List.of()) // 실제로는 UserService에서 사용자 정보 조회
                .isMyReaction(false) // 실제로는 현재 사용자 확인
                .build();
    }
}