package com.tcc.triAI_api_java.security;

import ca.uhn.fhir.context.FhirContext;
import com.tcc.triAI_api_java.controller.fhir.FhirExceptionHandler;
import com.tcc.triAI_api_java.controller.fhir.FhirJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

public class FhirAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String FHIR_PATH = "/api/fhir";

    private final FhirContext fhirContext;
    private final AuthenticationEntryPoint plain = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    public FhirAwareAuthenticationEntryPoint(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, jakarta.servlet.ServletException {
        if (!request.getRequestURI().startsWith(FHIR_PATH)) {
            plain.commence(request, response, exception);
            return;
        }

        OperationOutcome outcome = FhirExceptionHandler.outcome(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.LOGIN,
                List.of("Autenticação necessária: envie o token Bearer no cabeçalho Authorization."));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(FhirJson.MEDIA_TYPE + ";charset=UTF-8");
        response.getWriter().write(fhirContext.newJsonParser().encodeResourceToString(outcome));
    }
}
