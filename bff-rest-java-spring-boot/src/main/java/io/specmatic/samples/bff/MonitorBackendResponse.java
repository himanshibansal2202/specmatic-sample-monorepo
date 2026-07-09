package io.specmatic.samples.bff;

import java.util.List;
import java.util.Map;

public record MonitorBackendResponse(Integer statusCode, Map<String, Object> body, List<HeaderItem> headers) {
}
