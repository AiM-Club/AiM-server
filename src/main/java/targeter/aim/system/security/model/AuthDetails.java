package targeter.aim.system.security.model;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import targeter.aim.domain.user.entity.User;

import java.util.Collection;

public abstract class AuthDetails implements AuthenticatedPrincipal {
    public abstract String getKey();

    public String getName() {
        return this.getKey();
    }

    public abstract User getUser();

    public abstract Collection<? extends GrantedAuthority> getAuthorities();
}