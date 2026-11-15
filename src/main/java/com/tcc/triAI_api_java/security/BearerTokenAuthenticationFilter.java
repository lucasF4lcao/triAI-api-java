package com.tcc.triAI_api_java.security;

import com.tcc.triAI_api_java.entity.PersonalAccessToken;
import com.tcc.triAI_api_java.entity.User;
import com.tcc.triAI_api_java.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final PersonalAccessTokenService personalAccessTokenService;
    private final UserRepository userRepository;

    public BearerTokenAuthenticationFilter(
            PersonalAccessTokenService personalAccessTokenService,
            UserRepository userRepository
    ) {
        this.personalAccessTokenService = personalAccessTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String bearerToken = header.substring("Bearer ".length());

            Optional<PersonalAccessToken> personalAccessToken = personalAccessTokenService.resolve(bearerToken);

            if (personalAccessToken.isPresent()) {
                Optional<User> user = userRepository.findById(personalAccessToken.get().getTokenableId());

                if (user.isPresent() && user.get().isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.get(), null, user.get().getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute("currentPersonalAccessToken", personalAccessToken.get());

                    personalAccessTokenService.markUsed(personalAccessToken.get());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
