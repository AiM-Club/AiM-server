package targeter.aim.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.auth.dto.AuthDto;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.TierRepository;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TierRepository tierRepository;

    @Transactional
    public UserDto.UserResponse signUp(AuthDto.SignUpRequest request) {
        boolean isExisting = userRepository.existsByEmail(request.getEmail());
        if (isExisting){
            throw new RestException(ErrorCode.USER_ALREADY_EMAIL_EXISTS);
        }
        User toSave = request.toEntity(passwordEncoder);
        Tier bronze = tierRepository.findByName("BRONZE") // 신규 회원 기본 티어=BRONZE 설정
                .orElseThrow(() -> new RestException(ErrorCode.TIER_NOT_FOUND));
        toSave.setTier(bronze);

        User saved = userRepository.save(toSave);

        return UserDto.UserResponse.from(saved);
    }
}
