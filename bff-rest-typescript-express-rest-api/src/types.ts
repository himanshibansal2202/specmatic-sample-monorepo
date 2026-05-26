export type ProductType = "book" | "food" | "gadget" | "other";

export type ProductBase = {
  name: string;
  type: ProductType;
  inventory: number;
};

export type Product = ProductBase & {
  id: number;
  createdOn: string;
};

export type OrderBase = {
  productid: number;
  count: number;
};

export type BackendOrder = OrderBase & {
  id: number;
  status: "fulfilled" | "pending" | "cancelled";
};

export type BffOrder = OrderBase & {
  id: number;
  status: "pending" | "completed" | "cancelled";
};

export type IdResponse = {
  id: number;
};

export type ErrorResponse = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
};
