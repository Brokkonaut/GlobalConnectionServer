package de.cubeside.globalserver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientConfig {
    private String login;
    private String password;
    private boolean restricted;
    private Set<String> allowedChannels;

    public ClientConfig() {
    }

    public ClientConfig(String login, String password, boolean restricted, Set<String> allowedChannels) {
        this.login = login;
        this.password = password;
        this.restricted = restricted;
        this.allowedChannels = allowedChannels == null ? new HashSet<>() : new HashSet<>(allowedChannels);
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public Set<String> getAllowedChannels() {
        return allowedChannels;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public void setAllowedChannels(Set<String> allowedChannels) {
        this.allowedChannels = allowedChannels == null ? new HashSet<>() : new HashSet<>(allowedChannels);
    }

    public boolean checkPassword(byte[] password, byte[] saltServer, byte[] saltClient) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.password.getBytes(StandardCharsets.UTF_8));
            digest.update(saltServer);
            digest.update(saltClient);
            byte[] encodedhash = digest.digest();
            return Arrays.equals(password, encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);// impossible
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientConfig)) {
            return false;
        }
        return ((ClientConfig) obj).login.equals(login);
    }

    @Override
    public int hashCode() {
        return login.hashCode();
    }
}
