package com.tcc.triAI_api_java.repository;

import com.tcc.triAI_api_java.entity.PersonalAccessToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {

    Optional<PersonalAccessToken> findByToken(String token);
}
