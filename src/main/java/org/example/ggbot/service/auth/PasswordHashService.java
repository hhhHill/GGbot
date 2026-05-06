package org.example.ggbot.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.example.ggbot.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH = 256;

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] derived = derive(password, salt);
        return ITERATIONS
                + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(derived);
    }

    public boolean matches(String password, String storedHash) {
        String[] segments = storedHash.split(":");
        if (segments.length != 3) {
            throw new BadRequestException("Stored password hash is invalid");
        }
        int iterations = Integer.parseInt(segments[0]);
        byte[] salt = Base64.getDecoder().decode(segments[1].getBytes(StandardCharsets.UTF_8));
        byte[] expected = Base64.getDecoder().decode(segments[2].getBytes(StandardCharsets.UTF_8));
        byte[] actual = derive(password, salt, iterations);
        return java.security.MessageDigest.isEqual(expected, actual);
    }

    private byte[] derive(String password, byte[] salt) {
        return derive(password, salt, ITERATIONS);
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        }
    }
}
