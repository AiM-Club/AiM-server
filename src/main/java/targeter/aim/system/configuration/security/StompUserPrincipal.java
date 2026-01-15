package targeter.aim.system.configuration.security;

import java.security.Principal;

public class StompUserPrincipal implements Principal {

    private final Long userId;

    public StompUserPrincipal(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }
}
