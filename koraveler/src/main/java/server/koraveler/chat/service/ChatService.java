package server.koraveler.chat.service;

public interface ChatService {
    ChannelsDTO createChannel(ChannelsDTO channel) throws Exception;
    ChannelsDTO getChannelByUserId(String userId) throws Exception;
    ChannelsDTO updateChannel(ChannelsDTO channel) throws Exception;
    ChannelsDTO deleteChannel(String channelId) throws Exception;

}
