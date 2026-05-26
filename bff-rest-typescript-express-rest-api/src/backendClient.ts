import type { AppConfig } from "./config.js";
import type { BackendOrder, BffOrder, IdResponse, OrderBase, Product, ProductBase } from "./types.js";

export class BackendClient {
  constructor(private readonly config: AppConfig) {}

  async createProduct(product: ProductBase): Promise<IdResponse> {
    return this.requestJson<IdResponse>("/products", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        Authenticate: this.config.backendApiKey,
        "Idempotency-Key": crypto.randomUUID()
      },
      body: JSON.stringify(product)
    });
  }

  async findAvailableProducts(params: {
    type?: string;
    pageSize: string;
    fromDate: string;
    toDate: string;
  }): Promise<Product[]> {
    const search = new URLSearchParams();
    if (params.type !== undefined) {
      search.set("type", params.type);
    }
    search.set("from-date", params.fromDate);
    search.set("to-date", params.toDate);

    return this.requestJson<Product[]>(`/products?${search.toString()}`, {
      method: "GET",
      headers: {
        Accept: "application/json",
        pageSize: params.pageSize
      }
    });
  }

  async createOrder(order: OrderBase): Promise<IdResponse> {
    return this.requestJson<IdResponse>("/orders", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        Authenticate: this.config.backendApiKey,
        "Idempotency-Key": crypto.randomUUID()
      },
      body: JSON.stringify(order)
    });
  }

  async getOrders(): Promise<BffOrder[]> {
    const orders = await this.requestJson<BackendOrder[]>("/orders", {
      method: "GET",
      headers: {
        Accept: "application/json"
      }
    });

    return orders.map((order) => ({
      ...order,
      status: order.status === "fulfilled" ? "completed" : order.status
    }));
  }

  private async requestJson<T>(path: string, init: RequestInit): Promise<T> {
    const response = await fetch(`${this.config.backendBaseUrl}${path}`, init);
    const contentType = response.headers.get("content-type") ?? "";

    if (!response.ok) {
      const body = contentType.includes("application/json") ? await response.json() : await response.text();
      throw new BackendError(response.status, body);
    }

    if (!contentType.includes("application/json")) {
      throw new BackendError(502, {
        timestamp: new Date().toISOString(),
        status: 502,
        error: "Bad Gateway",
        message: "Backend returned a non-JSON response"
      });
    }

    return response.json() as Promise<T>;
  }
}

export class BackendError extends Error {
  constructor(
    readonly status: number,
    readonly body: unknown
  ) {
    super(`Backend request failed with status ${status}`);
  }
}
