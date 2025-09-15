package server.koraveler.chat.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import server.koraveler.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) throws Exception {

                        if (request instanceof ServletServerHttpRequest) {
                            HttpServletRequest servletRequest =
                                    ((ServletServerHttpRequest) request).getServletRequest();

                            // URL 파라미터 또는 헤더에서 토큰 추출
                            String token = servletRequest.getParameter("token");

                            // 토큰이 파라미터에 없으면 Authorization 헤더에서 추출
                            if (token == null || token.isEmpty()) {
                                String authHeader = servletRequest.getHeader("Authorization");
                                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                    token = authHeader.substring(7);
                                }
                            }

                            if (token == null || token.isEmpty()) {
                                log.error("No token provided in WebSocket connection");
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                return false;
                            }

                            try {
                                // 토큰 검증
                                Claims claims = jwtUtil.verifyToken(token);
                                String username = claims.getSubject();

                                // 검증 성공 시 속성에 저장
                                attributes.put("username", username);
                                attributes.put("token", token);

                                log.info("WebSocket connection authorized for user: {}", username);
                                return true;

                            } catch (ExpiredJwtException e) {
                                log.error("Expired token in WebSocket connection");
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                return false;

                            } catch (Exception e) {
                                log.error("Invalid token in WebSocket connection: {}", e.getMessage());
                                response.setStatusCode(HttpStatus.FORBIDDEN);
                                return false;
                            }
                        }

                        log.error("Invalid request type for WebSocket connection");
                        return false;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception exception) {
                        // 연결 후 처리 (필요시 로깅 등)
                        if (exception != null) {
                            log.error("WebSocket handshake failed: {}", exception.getMessage());
                        }
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Handshake에서 저장한 username 가져오기
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        String username = (String) sessionAttributes.get("username");

                        if (username != null) {
                            // Principal 설정
                            accessor.setUser(() -> username);
                            log.debug("STOMP connection established for user: {}", username);
                        } else {
                            log.error("No username found in session attributes");
                            throw new MessageDeliveryException("Unauthorized STOMP connection");
                        }
                    } else {
                        log.error("No session attributes found");
                        throw new MessageDeliveryException("Invalid STOMP connection");
                    }
                }

                // SUBSCRIBE, SEND 등 다른 명령에 대한 추가 검증 (선택사항)
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    String username = accessor.getUser() != null ? accessor.getUser().getName() : null;

                    log.debug("User {} subscribing to {}", username, destination);

                    // 필요시 구독 권한 검증
                    // 예: 특정 채널에 대한 구독 권한 확인
                    if (destination != null && destination.startsWith("/topic/channel/")) {
                        // channelService.validateUserAccess(username, channelId);
                    }
                }

                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
                    if (username == null) {
                        log.error("Unauthorized message send attempt");
                        throw new MessageDeliveryException("User not authenticated");
                    }
                }

                return message;
            }

            @Override
            public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
                // 메시지 전송 후 처리 (선택사항)
                if (!sent) {
                    StompHeaderAccessor accessor =
                            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    log.warn("Failed to send message: {}", accessor.getCommand());
                }
            }
        });
    }
}