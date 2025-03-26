package co.cyte.agent.core.domain;

/**
 * UserSession representa la sesión de un usuario autenticado.
 */
public class UserSession {
    private String username;
    private boolean authenticated;
    private String token;

    // Getters y setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "username='" + username + '\'' +
                ", authenticated=" + authenticated +
                ", token='" + token + '\'' +
                '}';
    }
}
