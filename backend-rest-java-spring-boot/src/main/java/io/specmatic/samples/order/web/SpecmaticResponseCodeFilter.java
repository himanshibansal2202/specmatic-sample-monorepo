package io.specmatic.samples.order.web;

import io.specmatic.samples.order.model.ErrorResponseBody;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

@Component
public class SpecmaticResponseCodeFilter extends HttpFilter {
    private static final Set<Integer> CONTRACT_ERRORS = Set.of(400, 404, 422);
    private final ObjectMapper objectMapper;

    public SpecmaticResponseCodeFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestedCode = request.getHeader("Specmatic-Response-Code");
        if (requestedCode != null) {
            try {
                int status = Integer.parseInt(requestedCode);
                if (CONTRACT_ERRORS.contains(status)) {
                    response.setStatus(status);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    String reason = switch (status) {
                        case 404 -> "Not Found";
                        case 422 -> "Unprocessable Entity";
                        default -> "Bad Request";
                    };
                    objectMapper.writeValue(response.getOutputStream(),
                            new ErrorResponseBody(Instant.now().toString(), status, reason, reason));
                    return;
                }
            } catch (NumberFormatException ignored) {
                // Non-numeric values are handled by the application as ordinary requests.
            }
        }
        chain.doFilter(request, response);
    }
}
