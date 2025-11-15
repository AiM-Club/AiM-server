package targeter.aim.system.security.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import targeter.aim.domain.user.entity.User;

import java.util.List;

@RequiredArgsConstructor
@Getter
@Builder
public class UserDetails extends AuthDetails{
    private final User user;

    @Override
    public String getKey() { return String.valueOf(user.getId());}

    public static UserDetails from(User user) {
        User unproxied = Hibernate.unproxy(user, User.class);
        // user.unproxy();

        return UserDetails.builder()
                .user(unproxied)
                .build();
    }

    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("User"));
    }
}