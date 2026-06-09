import { randomUUID } from "node:crypto";

function jsonHeaders(extra = {}) {
  return {
    Accept: "application/json",
    "Content-Type": "application/json",
    ...extra
  };
}

function dependencyHeaders(config, requestHeaders = {}) {
  const forwarded = {};
  if (requestHeaders.pagesize !== undefined) {
    forwarded.pageSize = requestHeaders.pagesize;
  }

  return {
    ...forwarded,
    Authenticate: config.backendAuth,
    "Idempotency-Key": requestHeaders["idempotency-key"] || config.idempotencyKey || randomUUID()
  };
}

async function parseDependencyResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : await response.text();
  return { status: response.status, body, contentType };
}

async function requestJson(url, options) {
  const response = await fetch(url, options);
  return parseDependencyResponse(response);
}

export class BackendClient {
  constructor(config) {
    this.config = config;
  }

  async createProduct(product, requestHeaders) {
    return requestJson(`${this.config.stubBaseUrl}/products`, {
      method: "POST",
      headers: jsonHeaders(dependencyHeaders(this.config, requestHeaders)),
      body: JSON.stringify(product)
    });
  }

  async findAvailableProducts(query, requestHeaders) {
    const url = new URL(`${this.config.stubBaseUrl}/products`);
    for (const key of ["type", "from-date", "to-date"]) {
      if (query[key] !== undefined) {
        url.searchParams.set(key, query[key]);
      }
    }

    const headers = {};
    if (requestHeaders.pagesize !== undefined) {
      headers.pageSize = requestHeaders.pagesize;
    }

    return requestJson(url, {
      method: "GET",
      headers: { Accept: "application/json", ...headers }
    });
  }

  async createOrder(order, requestHeaders) {
    return requestJson(`${this.config.stubBaseUrl}/orders`, {
      method: "POST",
      headers: jsonHeaders(dependencyHeaders(this.config, requestHeaders)),
      body: JSON.stringify(order)
    });
  }

  async getOrders(orderId) {
    const path = orderId === undefined ? "/orders" : `/orders/${orderId}`;
    return requestJson(`${this.config.stubBaseUrl}${path}`, {
      method: "GET",
      headers: { Accept: "application/json" }
    });
  }
}
