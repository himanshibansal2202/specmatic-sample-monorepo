package io.specmatic.samples.order.web;

import io.specmatic.samples.order.model.ErrorResponseBody;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponseBody> notFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class, org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    ResponseEntity<ErrorResponseBody> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    private ResponseEntity<ErrorResponseBody> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponseBody(Instant.now().toString(), status.value(), status.getReasonPhrase(), message));
    }
}
