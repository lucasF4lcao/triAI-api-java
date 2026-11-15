package com.tcc.triAI_api_java.service.triage;

public record MlSymptom(String codigo, String termo, String rotulo, Boolean selecionavel) {

    public boolean isSelectable() {
        return selecionavel == null || selecionavel;
    }
}
