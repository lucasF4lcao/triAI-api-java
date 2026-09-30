package com.tcc.triAI_api_java.controller.fhir;

import ca.uhn.fhir.parser.DataFormatException;
import com.tcc.triAI_api_java.exception.ResourceNotFoundException;
import com.tcc.triAI_api_java.exception.ValidationException;
import java.util.List;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice(basePackages = "com.tcc.triAI_api_java.controller.fhir")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FhirExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FhirExceptionHandler.class);

    private final FhirJson fhirJson;

    public FhirExceptionHandler(FhirJson fhirJson) {
        this.fhirJson = fhirJson;
    }

    public static OperationOutcome outcome(
            OperationOutcome.IssueSeverity severity,
            OperationOutcome.IssueType type,
            List<String> diagnostics
    ) {
        OperationOutcome outcome = new OperationOutcome();
        diagnostics.forEach(text -> outcome.addIssue().setSeverity(severity).setCode(type).setDiagnostics(text));
        return outcome;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> handleValidation(ValidationException exception) {
        List<String> messages = exception.getErrors().values().stream().flatMap(List::stream).toList();
        return fhirJson.respond(HttpStatus.UNPROCESSABLE_ENTITY,
                outcome(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.INVALID, messages));
    }

    @ExceptionHandler(DataFormatException.class)
    public ResponseEntity<String> handleFormat(DataFormatException exception) {
        return fhirJson.respond(HttpStatus.BAD_REQUEST,
                outcome(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.STRUCTURE,
                        List.of("JSON FHIR inválido: " + exception.getMessage())));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException exception) {
        return fhirJson.respond(HttpStatus.NOT_FOUND,
                outcome(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.NOTFOUND,
                        List.of(exception.getMessage())));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<String> handleUnavailable(RestClientException exception) {
        log.warn("Triage model service unavailable", exception);
        return fhirJson.respond(HttpStatus.SERVICE_UNAVAILABLE,
                outcome(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.TRANSIENT,
                        List.of("Serviço de classificação indisponível no momento. Tente novamente em instantes.")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception exception) {
        log.error("Unexpected error in FHIR endpoint", exception);
        return fhirJson.respond(HttpStatus.INTERNAL_SERVER_ERROR,
                outcome(OperationOutcome.IssueSeverity.FATAL, OperationOutcome.IssueType.EXCEPTION,
                        List.of("Erro interno do servidor")));
    }
}
