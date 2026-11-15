package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.entity.Change;
import com.tcc.triAI_api_java.entity.TriageAnalysis;
import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.exception.ResourceNotFoundException;
import com.tcc.triAI_api_java.repository.ChangeRepository;
import com.tcc.triAI_api_java.repository.TriageAnalysisRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TriageReviewService {

    private final TriageAnalysisRepository analysisRepository;
    private final ChangeRepository changeRepository;

    public TriageReviewService(TriageAnalysisRepository analysisRepository, ChangeRepository changeRepository) {
        this.analysisRepository = analysisRepository;
        this.changeRepository = changeRepository;
    }

    public record Review(TriageAnalysis analysis, List<Change> changesOldestFirst) {
    }

    @Transactional(readOnly = true)
    public Review find(Long analysisId) {
        TriageAnalysis analysis = findAnalysis(analysisId);
        return new Review(analysis, changesOldestFirst(analysis));
    }

    @Transactional
    public Review amend(Long analysisId, TriageColor finalColor, String reason) {
        TriageAnalysis analysis = findAnalysis(analysisId);

        Change change = new Change();
        change.setTriageAnalysis(analysis);
        change.setPredictedColor(analysis.getPredictedColor());
        change.setFinalColor(finalColor);
        change.setReason(reason);
        changeRepository.save(change);

        return new Review(analysis, changesOldestFirst(analysis));
    }

    private TriageAnalysis findAnalysis(Long id) {
        return analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment/" + id + " não encontrado"));
    }

    private List<Change> changesOldestFirst(TriageAnalysis analysis) {
        List<Change> changes = new ArrayList<>(changeRepository.findByTriageAnalysisOrderByIdDesc(analysis));
        Collections.reverse(changes);
        return changes;
    }
}
