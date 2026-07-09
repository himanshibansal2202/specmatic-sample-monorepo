export type ProductType = "book" | "food" | "gadget" | "other";
export type OrderStatus = "fulfilled" | "pending" | "cancelled";

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

export type Order = OrderBase & {
  id: number;
  status: OrderStatus;
};

export type OrderUpdate = OrderBase & {
  status: OrderStatus;
};

const today = "2023-10-01";

export class Store {
  private products = new Map<number, Product>();
  private orders = new Map<number, Order>();
  private nextProductId = 1000;
  private nextOrderId = 1000;

  constructor() {
    this.products.set(10, {
      id: 10,
      name: "XYZ Phone",
      type: "gadget",
      inventory: 10,
      createdOn: today
    });
    this.products.set(20, {
      id: 20,
      name: "Delete Candidate",
      type: "gadget",
      inventory: 10,
      createdOn: today
    });
    this.orders.set(10, {
      id: 10,
      productid: 10,
      count: 2,
      status: "pending"
    });
    this.orders.set(20, {
      id: 20,
      productid: 10,
      count: 2,
      status: "pending"
    });
  }

  listProducts(type?: ProductType, fromDate?: string, toDate?: string): Product[] {
    return Array.from(this.products.values()).filter((product) => {
      if (type && product.type !== type) return false;
      if (fromDate && product.createdOn < fromDate) return false;
      if (toDate && product.createdOn > toDate) return false;
      return true;
    });
  }

  getProduct(id: number): Product | undefined {
    return this.products.get(id);
  }

  createProduct(input: ProductBase): { id: number } {
    const id = this.nextProductId++;
    this.products.set(id, { id, ...input, createdOn: today });
    return { id };
  }

  updateProduct(id: number, input: ProductBase): boolean {
    const current = this.products.get(id);
    if (!current) return false;
    this.products.set(id, { id, ...input, createdOn: current.createdOn });
    return true;
  }

  deleteProduct(id: number): boolean {
    return this.products.delete(id);
  }

  listOrders(): Order[] {
    return Array.from(this.orders.values());
  }

  getOrder(id: number): Order | undefined {
    return this.orders.get(id);
  }

  createOrder(input: OrderBase): { id: number } | undefined {
    const product = this.products.get(input.productid);
    if (!product || product.inventory < input.count) return undefined;
    product.inventory -= input.count;
    const id = this.nextOrderId++;
    this.orders.set(id, { id, ...input, status: "pending" });
    return { id };
  }

  updateOrder(id: number, input: OrderUpdate): boolean {
    if (!this.orders.has(id)) return false;
    this.orders.set(id, { id, ...input });
    return true;
  }

  deleteOrder(id: number): boolean {
    return this.orders.delete(id);
  }
}
