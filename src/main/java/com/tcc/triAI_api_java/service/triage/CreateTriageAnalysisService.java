package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.entity.TriageAnalysis;
import com.tcc.triAI_api_java.entity.VitalSigns;
import com.tcc.triAI_api_java.enums.EntryType;
import com.tcc.triAI_api_java.enums.ExecutionStatus;
import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.fhir.FhirTriageExtractor;
import com.tcc.triAI_api_java.fhir.TriageInput;
import com.tcc.triAI_api_java.fhir.VitalSignsData;
import com.tcc.triAI_api_java.repository.TriageAnalysisRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTriageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CreateTriageAnalysisService.class);

    private final FhirTriageExtractor vitalSignsExtractor;
    private final TriageClassificationClient classificationClient;
    private final TriageAnalysisRepository triageAnalysisRepository;

    public CreateTriageAnalysisService(
            FhirTriageExtractor vitalSignsExtractor,
            TriageClassificationClient classificationClient,
            TriageAnalysisRepository triageAnalysisRepository
    ) {
        this.vitalSignsExtractor = vitalSignsExtractor;
        this.classificationClient = classificationClient;
        this.triageAnalysisRepository = triageAnalysisRepository;
    }

    @Transactional
    public TriageOutcome run(String bundleJson) {
        TriageInput input = vitalSignsExtractor.extract(bundleJson);
        VitalSignsData vitalSignsData = input.data();

        TriageClassificationResult classification;
        ExecutionStatus executionStatus;

        try {
            classification = classificationClient.classify(vitalSignsData);
            executionStatus = ExecutionStatus.SUCCESS;
        } catch (Exception exception) {
            log.error("Triage classification failed, falling back to red.", exception);
            classification = new TriageClassificationResult(TriageColor.RED, 0.0);
            executionStatus = ExecutionStatus.ERROR;
        }

        TriageAnalysis analysis = new TriageAnalysis();
        analysis.setEntryType(EntryType.FHIR);
        analysis.setPredictedColor(classification.color());
        analysis.setConfidence(BigDecimal.valueOf(classification.confidence()));
        analysis.setExecutionStatus(executionStatus);

        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setTriageAnalysis(analysis);
        vitalSigns.setAge(vitalSignsData.getAge());
        vitalSigns.setHeartRate(vitalSignsData.getHeartRate());
        vitalSigns.setSystolicPressure(vitalSignsData.getSystolicPressure());
        vitalSigns.setDiastolicPressure(vitalSignsData.getDiastolicPressure());
        vitalSigns.setOxygenSaturation(vitalSignsData.getOxygenSaturation());
        vitalSigns.setTemperature(vitalSignsData.getTemperature());
        vitalSigns.setRespiratoryRate(vitalSignsData.getRespiratoryRate());
        vitalSigns.setPain(vitalSignsData.getPain());

        analysis.setVitalSigns(vitalSigns);

        return new TriageOutcome(triageAnalysisRepository.save(analysis), input);
    }
}
