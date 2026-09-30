package com.tcc.triAI_api_java.fhir;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.ConceptMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ComplaintConceptMap {

    private static final String RESOURCE_PATH = "fhir/chief-complaint-conceptmap.json";

    private final ConceptMap conceptMap;
    private final Map<String, Set<String>> targetsBySourceCode = new HashMap<>();

    public ComplaintConceptMap(FhirContext fhirContext) {
        try (InputStream stream = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            this.conceptMap = fhirContext.newJsonParser()
                    .parseResource(ConceptMap.class, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load " + RESOURCE_PATH, exception);
        }

        for (ConceptMap.ConceptMapGroupComponent group : conceptMap.getGroup()) {
            for (ConceptMap.SourceElementComponent element : group.getElement()) {
                for (ConceptMap.TargetElementComponent target : element.getTarget()) {
                    targetsBySourceCode
                            .computeIfAbsent(key(group.getSource(), element.getCode()), ignored -> new LinkedHashSet<>())
                            .add(target.getCode());
                }
            }
        }
    }

    public Set<String> targetsOf(String system, String code) {
        return targetsBySourceCode.getOrDefault(key(system, code), Set.of());
    }

    public ConceptMap resource() {
        return conceptMap;
    }

    private static String key(String system, String code) {
        return system + "|" + code;
    }
}
