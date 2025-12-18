package targeter.aim.system.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.security.model.UserDetails;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLoadServiceImpl implements UserLoadService {
    private final UserRepository userRepository;

    @Override
    public Optional<UserDetails> loadUserByKey(String key) {
        return userRepository.findById(Long.valueOf(key))
                .map(UserDetails::from);
    }
}