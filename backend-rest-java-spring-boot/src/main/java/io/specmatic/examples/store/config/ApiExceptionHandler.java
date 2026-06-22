package io.specmatic.examples.store.config;

import io.specmatic.examples.store.model.ErrorResponseBody;
import io.specmatic.examples.store.service.ResourceNotFoundException;
import io.specmatic.examples.store.service.UnprocessableRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseBody> notFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UnprocessableRequestException.class)
    public ResponseEntity<ErrorResponseBody> unprocessable(UnprocessableRequestException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponseBody> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Bad request");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseBody> noResource(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Resource not found");
    }

    private ResponseEntity<ErrorResponseBody> error(HttpStatus status, String message) {
        ErrorResponseBody body = new ErrorResponseBody(
                OffsetDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(body);
    }
}
