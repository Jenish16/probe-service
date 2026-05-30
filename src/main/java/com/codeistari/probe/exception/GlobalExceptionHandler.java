package com.codeistari.probe.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message =
				ex.getBindingResult().getFieldErrors().stream()
						.map(this::formatFieldError)
						.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
	}

	@ExceptionHandler(ForgeRemoteCallException.class)
	public ResponseEntity<ErrorResponse> handleForgeRemoteCall(ForgeRemoteCallException ex) {
		return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(CallNotPermittedException.class)
	public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex) {
		String message =
				"Downstream circuit breaker is open: " + ex.getCausingCircuitBreakerName();
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(message));
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
