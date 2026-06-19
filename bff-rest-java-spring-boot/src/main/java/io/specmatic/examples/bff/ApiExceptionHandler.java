package io.specmatic.examples.bff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      MissingServletRequestParameterException.class,
      MissingRequestHeaderException.class,
      MethodArgumentTypeMismatchException.class,
      HttpMessageNotReadableException.class,
      ConstraintViolationException.class,
      IllegalArgumentException.class
  })
  ResponseEntity<BadRequest> badRequest(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(HttpClientErrorException.class)
  ResponseEntity<String> dependencyError(HttpClientErrorException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .headers(exception.getResponseHeaders())
        .body(exception.getResponseBodyAsString());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<BadRequest> fallback(Exception exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  private ResponseEntity<BadRequest> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new BadRequest(
        OffsetDateTime.now().toString(),
        status.value(),
        status.getReasonPhrase(),
        message == null ? status.getReasonPhrase() : message));
  }
}
