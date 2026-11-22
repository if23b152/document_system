package at.technikum_wien.rest_server.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

/** Authentication of a request made with a personal access token instead of a session. */
public class ApiTokenAuthentication extends AbstractAuthenticationToken {

    private final UserDetails user;
    private final Long tokenId;

    public ApiTokenAuthentication(UserDetails user, Long tokenId) {
        super(user.getAuthorities());
        this.user = user;
        this.tokenId = tokenId;
        setAuthenticated(true);
    }

    public Long getTokenId() {
        return tokenId;
    }

    @Override
    public Object getCredentials() {
        // The secret is never kept in memory after the check
        return null;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }
}
