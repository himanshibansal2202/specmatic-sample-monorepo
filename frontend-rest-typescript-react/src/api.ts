export type ProductType = "book" | "food" | "gadget" | "other";

export interface ProductBase {
  name: string;
  type: ProductType;
  inventory: number;
}

export interface Product extends ProductBase {
  id: number;
  createdOn: string;
}

export interface OrderBase {
  productid: number;
  count: number;
}

export interface Order extends OrderBase {
  id: number;
  status: "pending" | "completed" | "cancelled";
}

export interface IdResponse {
  id: number;
}

export interface MonitorResponse {
  request?: {
    method: string;
    body: Record<string, unknown>;
    headers: Array<{ name?: string; value?: string }>;
  };
  response?: {
    statusCode: number;
    body: Record<string, unknown>;
    headers: Array<{ name?: string; value?: string }>;
  };
}

export interface SearchProductsRequest {
  type?: ProductType;
  pageSize: number;
  fromDate: string;
  toDate: string;
}

export class BffApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly headers: Headers
  ) {
    super(message);
    this.name = "BffApiError";
  }
}

const jsonHeaders = {
  "Content-Type": "application/json",
  Accept: "application/json"
};

async function parseJson<T>(response: Response): Promise<T> {
  if (response.status === 202) {
    return { id: extractMonitorId(response.headers.get("Link")) } as T;
  }

  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("application/json")) {
    throw new BffApiError(`Expected JSON from BFF, received ${contentType || "no content type"}`, response.status, response.headers);
  }

  return (await response.json()) as T;
}

function extractMonitorId(link: string | null): number {
  const match = link?.match(/\/monitor\/(\d+)/);
  return match ? Number(match[1]) : 0;
}

export function createBffClient(baseUrl: string) {
  const root = baseUrl.replace(/\/$/, "");

  async function checkedFetch<T>(path: string, init: RequestInit): Promise<T> {
    const response = await fetch(`${root}${path}`, init);
    if (!response.ok && response.status !== 202) {
      throw new BffApiError(`BFF request failed with HTTP ${response.status}`, response.status, response.headers);
    }
    return parseJson<T>(response);
  }

  return {
    createProduct(product: ProductBase): Promise<IdResponse> {
      return checkedFetch<IdResponse>("/products", {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify(product)
      });
    },

    findAvailableProducts(request: SearchProductsRequest): Promise<Product[]> {
      const params = new URLSearchParams({
        "from-date": request.fromDate,
        "to-date": request.toDate
      });

      if (request.type) {
        params.set("type", request.type);
      }

      return checkedFetch<Product[]>(`/findAvailableProducts?${params.toString()}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
          pageSize: String(request.pageSize)
        }
      });
    },

    createOrder(order: OrderBase): Promise<IdResponse> {
      return checkedFetch<IdResponse>("/orders", {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify(order)
      });
    },

    getOrders(orderId?: number): Promise<Order[]> {
      const path = orderId ? `/orders?orderId=${orderId}` : "/orders";
      return checkedFetch<Order[]>(path, {
        method: "GET",
        headers: { Accept: "application/json" }
      });
    },

    retrieveMonitor(id: number): Promise<MonitorResponse> {
      return checkedFetch<MonitorResponse>(`/monitor/${id}`, {
        method: "GET",
        headers: { Accept: "application/json" }
      });
    }
  };
}
