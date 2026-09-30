package com.tcc.triAI_api_java.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin@triai.local") @NotBlank @Email String email,
        @Schema(example = "sua-senha") @NotBlank String password
) {
}
