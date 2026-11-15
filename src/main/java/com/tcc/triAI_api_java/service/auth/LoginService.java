package com.tcc.triAI_api_java.service.auth;

import com.tcc.triAI_api_java.dto.response.LoginResponse;
import com.tcc.triAI_api_java.dto.response.UserResponse;
import com.tcc.triAI_api_java.entity.User;
import com.tcc.triAI_api_java.exception.ValidationException;
import com.tcc.triAI_api_java.security.PersonalAccessTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final PersonalAccessTokenService personalAccessTokenService;

    public LoginService(
            AuthenticationManager authenticationManager,
            PersonalAccessTokenService personalAccessTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.personalAccessTokenService = personalAccessTokenService;
    }

    public LoginResponse run(String email, String password) {
        User user;

        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            user = (User) authentication.getPrincipal();
        } catch (DisabledException exception) {
            throw ValidationException.of("email", "Usuário inativo");
        } catch (BadCredentialsException | UsernameNotFoundException exception) {
            throw ValidationException.of("email", "Credenciais inválidas");
        }

        String token = personalAccessTokenService.issueToken(user, "api");

        return new LoginResponse(token, UserResponse.from(user));
    }
}
