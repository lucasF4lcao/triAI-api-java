package com.tcc.triAI_api_java.service.auth;

import com.tcc.triAI_api_java.entity.PersonalAccessToken;
import com.tcc.triAI_api_java.security.PersonalAccessTokenService;
import org.springframework.stereotype.Service;

@Service
public class LogoutService {

    private final PersonalAccessTokenService personalAccessTokenService;

    public LogoutService(PersonalAccessTokenService personalAccessTokenService) {
        this.personalAccessTokenService = personalAccessTokenService;
    }

    public void run(PersonalAccessToken token) {
        personalAccessTokenService.revoke(token);
    }
}
