package com.tcc.triAI_api_java.service.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.fhir.VitalSignsData;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TriageClassificationClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://ml.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final TriageClassificationClient client = new TriageClassificationClient(builder.build());

    private void respondWith(String json) {
        server.expect(requestTo("http://ml.test/classificar"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void readsTheColorTheConfidenceTheProbabilitiesAndTheSafetyFlag() {
        respondWith("""
                {"corPrevista": "Verde", "confianca": 0.6,
                 "probabilidades": {"Verde": 0.6, "Amarelo": 0.4}, "regraDeSeguranca": false}
                """);

        TriageClassificationResult result = client.classify(new VitalSignsData());

        assertThat(result.color()).isEqualTo(TriageColor.GREEN);
        assertThat(result.confidence()).isEqualTo(0.6);
        assertThat(result.modelOutput().probabilities())
                .containsEntry(TriageColor.GREEN, 0.6)
                .containsEntry(TriageColor.YELLOW, 0.4);
        assertThat(result.modelOutput().safetyRuleApplied()).isFalse();
    }

    @Test
    void readsTheSafetyRuleFlag() {
        respondWith("""
                {"corPrevista": "Vermelho", "confianca": 0.7,
                 "probabilidades": {"Amarelo": 0.7, "Vermelho": 0.1}, "regraDeSeguranca": true}
                """);

        assertThat(client.classify(new VitalSignsData()).modelOutput().safetyRuleApplied()).isTrue();
    }

    @Test
    void stillWorksWithAnOlderMlResponseWithoutTheNewFields() {
        respondWith("{\"corPrevista\": \"Amarelo\", \"confianca\": 0.5}");

        TriageClassificationResult result = client.classify(new VitalSignsData());

        assertThat(result.color()).isEqualTo(TriageColor.YELLOW);
        assertThat(result.modelOutput().probabilities()).isEmpty();
        assertThat(result.modelOutput().safetyRuleApplied()).isFalse();
    }
}
