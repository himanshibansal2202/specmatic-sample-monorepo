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

export interface HeaderItem {
  name?: string;
  value?: string;
}

export interface MonitorResponse {
  request?: {
    method: string;
    body: Record<string, unknown>;
    headers: HeaderItem[];
  };
  response?: {
    statusCode: number;
    body: Record<string, unknown>;
    headers: HeaderItem[];
  };
}

export interface FindAvailableProductsRequest {
  type?: ProductType;
  pageSize: number;
  fromDate: string;
  toDate: string;
}

export class OrderBffClient {
  constructor(private readonly baseUrl: string) {}

  async createProduct(product: ProductBase, expectedStatus = 201): Promise<IdResponse | null> {
    const response = await fetch(this.url("/products"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Specmatic-Response-Code": String(expectedStatus)
      },
      body: JSON.stringify(product)
    });

    if (response.status === 202) {
      return null;
    }

    return this.parseJson<IdResponse>(response, expectedStatus);
  }

  async findAvailableProducts(request: FindAvailableProductsRequest): Promise<Product[]> {
    const query = new URLSearchParams({
      "from-date": request.fromDate,
      "to-date": request.toDate
    });

    if (request.type !== undefined) {
      query.set("type", request.type);
    }

    const response = await fetch(this.url(`/findAvailableProducts?${query.toString()}`), {
      method: "GET",
      headers: {
        pageSize: String(request.pageSize)
      }
    });

    return this.parseJson<Product[]>(response, 200);
  }

  async createOrder(order: OrderBase, expectedStatus = 201): Promise<IdResponse | null> {
    const response = await fetch(this.url("/orders"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Specmatic-Response-Code": String(expectedStatus)
      },
      body: JSON.stringify(order)
    });

    if (response.status === 202) {
      return null;
    }

    return this.parseJson<IdResponse>(response, expectedStatus);
  }

  async getOrders(orderId?: number): Promise<Order[]> {
    const path = orderId === undefined ? "/orders" : `/orders?orderId=${orderId}`;
    const response = await fetch(this.url(path), { method: "GET" });
    return this.parseJson<Order[]>(response, 200);
  }

  async retrieveMonitor(id: number): Promise<MonitorResponse> {
    const response = await fetch(this.url(`/monitor/${id}`), { method: "GET" });
    return this.parseJson<MonitorResponse>(response, 200);
  }

  private url(path: string): string {
    return new URL(path, this.baseUrl).toString();
  }

  private async parseJson<T>(response: Response, expectedStatus: number): Promise<T> {
    if (response.status !== expectedStatus) {
      const body = await response.text();
      throw new Error(`Expected ${expectedStatus}, got ${response.status}: ${body}`);
    }

    return response.json() as Promise<T>;
  }
}
