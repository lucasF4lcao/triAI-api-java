package com.tcc.triAI_api_java.security;

import com.tcc.triAI_api_java.entity.PersonalAccessToken;
import com.tcc.triAI_api_java.entity.User;
import com.tcc.triAI_api_java.repository.PersonalAccessTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PersonalAccessTokenService {

    private static final String TOKENABLE_TYPE = "App\\Models\\User";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PersonalAccessTokenRepository personalAccessTokenRepository;

    public PersonalAccessTokenService(PersonalAccessTokenRepository personalAccessTokenRepository) {
        this.personalAccessTokenRepository = personalAccessTokenRepository;
    }

    public String issueToken(User user, String name) {
        String plainTextToken = generatePlainTextToken();

        PersonalAccessToken token = new PersonalAccessToken();
        token.setTokenableType(TOKENABLE_TYPE);
        token.setTokenableId(user.getId());
        token.setName(name);
        token.setToken(hash(plainTextToken));

        token = personalAccessTokenRepository.save(token);

        return token.getId() + "|" + plainTextToken;
    }

    public Optional<PersonalAccessToken> resolve(String bearerToken) {
        int separatorIndex = bearerToken.indexOf('|');

        if (separatorIndex < 0) {
            return Optional.empty();
        }

        String plainTextToken = bearerToken.substring(separatorIndex + 1);

        return personalAccessTokenRepository.findByToken(hash(plainTextToken))
                .filter(token -> token.getExpiresAt() == null || token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    public void markUsed(PersonalAccessToken token) {
        token.setLastUsedAt(LocalDateTime.now());
        personalAccessTokenRepository.save(token);
    }

    public void revoke(PersonalAccessToken token) {
        personalAccessTokenRepository.delete(token);
    }

    private String generatePlainTextToken() {
        StringBuilder builder = new StringBuilder(40);
        for (int i = 0; i < 40; i++) {
            builder.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return builder.toString();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
