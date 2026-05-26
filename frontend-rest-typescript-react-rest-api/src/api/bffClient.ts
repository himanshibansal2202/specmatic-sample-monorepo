import type { ProductBase, Product, OrderBase, Order, Id, MonitorResponse } from "../types/index.js";

const BFF_BASE_URL = process.env.BFF_BASE_URL || "http://localhost:8090";

export async function createProduct(product: ProductBase): Promise<Id> {
  const res = await fetch(`${BFF_BASE_URL}/products`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(product),
  });
  if (!res.ok) throw new Error(`Create product failed: ${res.status}`);
  return res.json() as Promise<Id>;
}

export async function findAvailableProducts(params: {
  type?: string;
  pageSize: number;
  fromDate: string;
  toDate: string;
}): Promise<Product[]> {
  const query = new URLSearchParams();
  if (params.type) query.set("type", params.type);
  query.set("from-date", params.fromDate);
  query.set("to-date", params.toDate);
  const res = await fetch(`${BFF_BASE_URL}/findAvailableProducts?${query}`, {
    headers: { pageSize: String(params.pageSize) },
  });
  if (!res.ok) throw new Error(`Find products failed: ${res.status}`);
  return res.json() as Promise<Product[]>;
}

export async function createOrder(order: OrderBase): Promise<Id> {
  const res = await fetch(`${BFF_BASE_URL}/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(order),
  });
  if (!res.ok) throw new Error(`Create order failed: ${res.status}`);
  return res.json() as Promise<Id>;
}

export async function getOrders(orderId?: number): Promise<Order[]> {
  const query = orderId !== undefined ? `?orderId=${orderId}` : "";
  const res = await fetch(`${BFF_BASE_URL}/orders${query}`);
  if (!res.ok) throw new Error(`Get orders failed: ${res.status}`);
  return res.json() as Promise<Order[]>;
}

export async function getMonitor(id: number): Promise<MonitorResponse> {
  const res = await fetch(`${BFF_BASE_URL}/monitor/${id}`);
  if (!res.ok) throw new Error(`Get monitor failed: ${res.status}`);
  return res.json() as Promise<MonitorResponse>;
}
