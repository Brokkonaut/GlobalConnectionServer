package de.cubeside.globalserver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ClientConfig {
    private String login;
    private String password;

    public ClientConfig(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
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
