package targeter.aim.system.security.service;

import targeter.aim.system.security.model.AuthDetails;

import java.util.Optional;

public interface UserLoadService {
    Optional<? extends AuthDetails> loadUserByKey(String key);
}