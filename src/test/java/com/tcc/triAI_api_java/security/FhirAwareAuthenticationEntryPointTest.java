package com.tcc.triAI_api_java.security;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class FhirAwareAuthenticationEntryPointTest {

    private final FhirAwareAuthenticationEntryPoint entryPoint = new FhirAwareAuthenticationEntryPoint(FhirContext.forR4());

    @Test
    void answersFhirPathsWithAnOperationOutcome() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest("POST", "/api/fhir/$triage"), response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/fhir+json");
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("\"resourceType\":\"OperationOutcome\"")
                .contains("\"code\":\"login\"");
    }

    @Test
    void keepsAPlain401ForTheOtherPaths() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest("GET", "/api/user"), response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEmpty();
    }
}
