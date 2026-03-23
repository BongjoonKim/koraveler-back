package server.nadeliv.users.service.EmailVerificationServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import server.nadeliv.users.service.EmailVerificationService;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final StringRedisTemplate redisTemplate;
    private final EmailSer emailService;
}
