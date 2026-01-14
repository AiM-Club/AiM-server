package targeter.aim.system.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.security.utility.jwt.JwtTokenResolver;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenResolver jwtTokenResolver;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        accessor.setLeaveMutable(true);

        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        if (command == StompCommand.CONNECT) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalStateException("WebSocket CONNECT Authorization 없음");
            }

            String token = authHeader.substring(7);
            jwtTokenResolver.validateToken(token);

            Long userId = Long.valueOf(
                    jwtTokenResolver.resolveTokenFromString(token).getSubject()
            );

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            // Channel principal (선택)
            accessor.setUser(new StompUserPrincipal(user.getId()));

            accessor.getSessionAttributes().put("userId", user.getId());

            return message;
        }

        return message;
    }
}