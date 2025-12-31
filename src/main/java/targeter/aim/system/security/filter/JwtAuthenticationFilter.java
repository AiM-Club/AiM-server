package targeter.aim.system.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import targeter.aim.domain.auth.token.validator.RefreshTokenValidator;
import targeter.aim.system.security.exception.JwtAuthenticationException;
import targeter.aim.system.security.exception.JwtBlacklistedTokenException;
import targeter.aim.system.security.exception.JwtInvalidTokenException;
import targeter.aim.system.security.exception.JwtTokenMissingException;
import targeter.aim.system.security.initializer.JwtAuthPathInitializer;
import targeter.aim.system.security.model.ApiPathPattern;
import targeter.aim.system.security.service.UserLoadService;
import targeter.aim.system.security.utility.jwt.JwtTokenResolver;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final List<ApiPathPattern> ignorePatterns;
    private final List<ApiPathPattern> allowedPatterns;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final JwtTokenResolver jwtTokenResolver;
    private final UserLoadService userLoadService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final RefreshTokenValidator refreshTokenValidator;
    private final JwtAuthPathInitializer jwtAuthPathInitializer;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String servletPath = request.getServletPath();
        boolean requiresAuth = this.isMatchingURI(servletPath, request.getMethod());

        try {
            authenticateWithJwt(request, requiresAuth);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (requiresAuth) {
                handleAuthenticationError(request, response, e);
            } else {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
            }
        }
    }

    private void authenticateWithJwt(HttpServletRequest request, boolean requiresAuth) {
        var accessTokenOpt = jwtTokenResolver.parseTokenFromRequest(request);

        if (accessTokenOpt.isEmpty()) {
            if (requiresAuth) {
                throw new JwtTokenMissingException();
            }
            return;
        }

        String accessToken = accessTokenOpt.get();

        if (!jwtTokenResolver.validateToken(accessToken)) {
            if (requiresAuth) {
                throw new JwtBlacklistedTokenException();
            }
            return;
        }

        var parsedTokenData = jwtTokenResolver.resolveTokenFromString(accessToken);
        var userDetails = userLoadService.loadUserByKey(parsedTokenData.getSubject());

        if (userDetails.isEmpty()) {
            if (requiresAuth) {
                throw new JwtInvalidTokenException();
            }
            return;
        }

        refreshTokenValidator.validateOrThrow(userDetails.get().getKey(), parsedTokenData.getRefreshUuid());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails.get(),
                        null,
                        userDetails.get().getAuthorities()
                )
        );
    }

    private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        if (e instanceof JwtAuthenticationException) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        } else {
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new JwtAuthenticationException("Authentication failed", 401, e)
            );
        }
    }

    private boolean isMatchingURI(String servletPath, String method) {
        ApiPathPattern.METHODS apiMethod = ApiPathPattern.METHODS.parse(method);
        if (apiMethod == null) return false;

        boolean isAllowed = allowedPatterns.stream()
                .anyMatch(p -> antPathMatcher.match(p.getPattern(), servletPath) && p.getMethod() == apiMethod);

        boolean isIgnored = ignorePatterns.stream()
                .anyMatch(p -> antPathMatcher.match(p.getPattern(), servletPath) && p.getMethod() == apiMethod);

        boolean isExcluded = jwtAuthPathInitializer.getExcludePaths().stream()
                .anyMatch(p -> antPathMatcher.match(p.getPattern(), servletPath) && p.getMethod() == apiMethod);

        boolean isConflicting = jwtAuthPathInitializer.getConflictPaths().stream()
                .anyMatch(p -> antPathMatcher.match(p.getPattern(), servletPath) && p.getMethod() == apiMethod);

        return isAllowed && !isIgnored && (!isExcluded || isConflicting);
    }
}