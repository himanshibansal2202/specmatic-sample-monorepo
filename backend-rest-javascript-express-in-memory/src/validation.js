const productTypes = new Set(["book", "food", "gadget", "other"]);
const orderStatuses = new Set(["fulfilled", "pending", "cancelled"]);

export function parseId(value) {
  const id = Number(value);
  if (!Number.isInteger(id)) return null;
  return id;
}

export function errorBody(status, error, message) {
  return {
    timestamp: new Date(0).toISOString(),
    status,
    error,
    message
  };
}

export function validateProductBase(body) {
  if (!body || typeof body !== "object") return "Request body must be an object";
  if (typeof body.name !== "string") return "name is required";
  if (!productTypes.has(body.type)) return "type must be one of book, food, gadget, other";
  if (!Number.isInteger(body.inventory) || body.inventory < 1 || body.inventory > 101) {
    return "inventory must be an integer from 1 to 101";
  }
  return null;
}

export function validateOrderBase(body) {
  if (!body || typeof body !== "object") return "Request body must be an object";
  if (!Number.isInteger(body.productid)) return "productid is required";
  if (!Number.isInteger(body.count)) return "count is required";
  return null;
}

export function validateOrderUpdate(body) {
  const baseError = validateOrderBase(body);
  if (baseError) return baseError;
  if (!orderStatuses.has(body.status)) {
    return "status must be one of fulfilled, pending, cancelled";
  }
  return null;
}

export function isValidDate(value) {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

export function isValidProductType(value) {
  return !value || productTypes.has(value);
}

export function isValidUuid(value) {
  return (
    typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
  );
}

export function isValidIntegerHeader(value) {
  return value === undefined || /^-?\d+$/.test(value);
}
