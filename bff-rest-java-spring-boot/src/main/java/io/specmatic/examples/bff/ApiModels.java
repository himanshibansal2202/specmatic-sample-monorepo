package io.specmatic.examples.bff;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

record ProductBase(
    @NotNull Object name,
    @NotNull ProductType type,
    @NotNull @Min(1) @Max(101) Integer inventory) {}

record Product(Integer id, String name, ProductType type, Integer inventory, String createdOn) {}

record Id(Integer id) {}

record OrderBase(@NotNull Integer productid, @NotNull Integer count) {}

record Order(Integer id, Integer productid, Integer count, String status) {}

record BadRequest(String timestamp, Integer status, String error, String message) {}

record MonitorResponse(MonitorRequest request, MonitorResult response) {}

record MonitorRequest(String method, Map<String, Object> body, List<HeaderItem> headers) {}

record MonitorResult(Integer statusCode, Map<String, Object> body, List<HeaderItem> headers) {}

record HeaderItem(String name, String value) {}

enum ProductType {
  book,
  food,
  gadget,
  other
}
