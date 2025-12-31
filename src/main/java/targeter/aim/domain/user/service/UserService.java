package targeter.aim.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto.UserResponse getMe(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_TOKEN_NOT_FOUND);
        }

        var user = userRepository.findById(Long.valueOf(userDetails.getKey()))
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        return UserDto.UserResponse.from(user);
    }
}
