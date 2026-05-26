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

export interface Id {
  id: number;
}

export interface MonitorResponse {
  request: {
    method: string;
    body: object;
    headers: { name: string; value: string }[];
  };
  response: {
    statusCode: number;
    body: object;
    headers: { name: string; value: string }[];
  };
}
