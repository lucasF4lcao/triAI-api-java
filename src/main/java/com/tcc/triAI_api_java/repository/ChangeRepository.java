package com.tcc.triAI_api_java.repository;

import com.tcc.triAI_api_java.entity.Change;
import com.tcc.triAI_api_java.entity.TriageAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRepository extends JpaRepository<Change, Long> {

    List<Change> findByTriageAnalysisOrderByIdDesc(TriageAnalysis triageAnalysis);
}
