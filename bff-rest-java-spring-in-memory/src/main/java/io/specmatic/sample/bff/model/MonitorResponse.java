package io.specmatic.sample.bff.model;

import java.util.List;
import java.util.Map;

public record MonitorResponse(MonitorRequest request, MonitorResult response) {
    public record MonitorRequest(String method, Map<String, Object> body, List<HeaderItem> headers) {
    }

    public record MonitorResult(Integer statusCode, Map<String, Object> body, List<HeaderItem> headers) {
    }
}
