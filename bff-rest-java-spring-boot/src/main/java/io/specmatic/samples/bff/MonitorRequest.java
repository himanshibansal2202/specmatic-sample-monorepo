package io.specmatic.samples.bff;

import java.util.List;
import java.util.Map;

public record MonitorRequest(String method, Map<String, Object> body, List<HeaderItem> headers) {
}
