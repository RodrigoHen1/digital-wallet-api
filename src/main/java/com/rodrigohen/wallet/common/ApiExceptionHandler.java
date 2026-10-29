package com.rodrigohen.wallet.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 "problem detail" JSON responses, so every error has the same shape.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	ProblemDetail handleUnauthorized(UnauthorizedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Business rule violated", ex.getMessage());
	}

	// Safety net for races the service checks cannot see, e.g. two requests with the same email at once
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult()
			.getFieldErrors()
			.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return invalidRequest(errors);
	}

	// Raised instead of the above when a handler also validates headers or query params (e.g. Idempotency-Key)
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			if (result instanceof ParameterErrors parameterErrors) {
				parameterErrors.getFieldErrors()
					.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
			}
			else {
				String parameter = result.getMethodParameter().getParameterName();
				result.getResolvableErrors()
					.forEach(error -> errors.putIfAbsent(parameter, error.getDefaultMessage()));
			}
		}
		return invalidRequest(errors);
	}

	private static ResponseEntity<Object> invalidRequest(Map<String, String> errors) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "One or more fields are invalid");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
