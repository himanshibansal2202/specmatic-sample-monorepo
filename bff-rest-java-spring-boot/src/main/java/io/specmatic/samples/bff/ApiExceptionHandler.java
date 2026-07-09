package io.specmatic.samples.bff;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class,
      MissingRequestHeaderException.class,
      NoResourceFoundException.class
  })
  ResponseEntity<ErrorResponse> badRequest(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> generic(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(
        OffsetDateTime.now().toString(),
        status.value(),
        status.getReasonPhrase(),
        message == null ? status.getReasonPhrase() : message
    ));
  }
}
