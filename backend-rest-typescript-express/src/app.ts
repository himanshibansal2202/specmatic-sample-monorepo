import express, { type Request, type Response } from "express";
import multer from "multer";
import { Store, type OrderStatus, type ProductType } from "./store.js";

const productTypes = new Set<ProductType>(["book", "food", "gadget", "other"]);
const orderStatuses = new Set<OrderStatus>(["fulfilled", "pending", "cancelled"]);

type ErrorStatus = 400 | 404 | 422;

function errorBody(status: ErrorStatus, message: string) {
  return {
    timestamp: new Date(0).toISOString(),
    status,
    error: status === 404 ? "Not Found" : status === 422 ? "Unprocessable Entity" : "Bad Request",
    message
  };
}

function sendError(res: Response, status: ErrorStatus, message: string) {
  return res.status(status).type("application/json").send(errorBody(status, message));
}

function parseInteger(value: unknown): number | undefined {
  if (typeof value !== "string" || !/^-?\d+$/.test(value)) return undefined;
  return Number(value);
}

function isUuid(value: string | undefined): boolean {
  return typeof value === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

function isDate(value: string | undefined): boolean {
  if (value === undefined) return true;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

function requestProductBase(body: unknown) {
  const candidate = body as Record<string, unknown>;
  if (!candidate || typeof candidate.name !== "string") return undefined;
  if (typeof candidate.type !== "string" || !productTypes.has(candidate.type as ProductType)) return undefined;
  if (typeof candidate.inventory !== "number" || candidate.inventory < 1 || candidate.inventory > 101) return undefined;
  return {
    name: candidate.name,
    type: candidate.type as ProductType,
    inventory: candidate.inventory
  };
}

function requestOrderBase(body: unknown) {
  const candidate = body as Record<string, unknown>;
  if (!candidate || typeof candidate.productid !== "number" || typeof candidate.count !== "number") return undefined;
  return {
    productid: candidate.productid,
    count: candidate.count
  };
}

function requestOrderUpdate(body: unknown) {
  const base = requestOrderBase(body);
  const candidate = body as Record<string, unknown>;
  if (!base || typeof candidate.status !== "string" || !orderStatuses.has(candidate.status as OrderStatus)) {
    return undefined;
  }
  return {
    ...base,
    status: candidate.status as OrderStatus
  };
}

export function createApp(store = new Store()) {
  const app = express();
  const upload = multer({ storage: multer.memoryStorage() });

  app.use(express.json());

  app.get("/products", (req, res) => {
    const { type, "from-date": fromDate, "to-date": toDate } = req.query;
    if (type !== undefined && (typeof type !== "string" || !productTypes.has(type as ProductType))) {
      return sendError(res, 400, "Invalid product type");
    }
    if (fromDate !== undefined && typeof fromDate !== "string") return sendError(res, 400, "Invalid from-date");
    if (toDate !== undefined && typeof toDate !== "string") return sendError(res, 400, "Invalid to-date");
    if (!isDate(fromDate)) return sendError(res, 400, "Invalid from-date");
    if (!isDate(toDate)) return sendError(res, 400, "Invalid to-date");
    const pageSize = req.header("pageSize");
    if (pageSize !== undefined && parseInteger(pageSize) === undefined) return sendError(res, 400, "Invalid pageSize");
    return res.status(200).type("application/json").send(store.listProducts(type as ProductType | undefined, fromDate, toDate));
  });

  app.post("/products", (req, res) => {
    if (!isUuid(req.header("Idempotency-Key"))) return sendError(res, 400, "Invalid Idempotency-Key");
    const input = requestProductBase(req.body);
    if (!input) return sendError(res, 400, "Invalid product");
    return res.status(201).type("application/json").send(store.createProduct(input));
  });

  app.get("/products/:id", (req, res) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid product id");
    const product = store.getProduct(id);
    if (!product) return sendError(res, 404, "Product not found");
    return res.status(200).type("application/json").send(product);
  });

  app.patch("/products/:id", (req, res) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid product id");
    const input = requestProductBase(req.body);
    if (!input) return sendError(res, 400, "Invalid product");
    if (!store.updateProduct(id, input)) return sendError(res, 404, "Product not found");
    return res.status(200).type("text/plain").send("success");
  });

  app.delete("/products/:id", (req, res) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid product id");
    if (!store.deleteProduct(id)) return sendError(res, 404, "Product not found");
    return res.status(200).type("text/plain").send("success");
  });

  app.put("/products/:id/image", upload.single("image"), (req: Request, res: Response) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid product id");
    if (!store.getProduct(id)) return sendError(res, 404, "Product not found");
    if (!req.file) return sendError(res, 400, "Missing image");
    return res.status(200).type("application/json").send({ message: "Success" });
  });

  app.get("/orders", (_req, res) => {
    return res.status(200).type("application/json").send(store.listOrders());
  });

  app.post("/orders", (req, res) => {
    if (!isUuid(req.header("Idempotency-Key"))) return sendError(res, 400, "Invalid Idempotency-Key");
    const input = requestOrderBase(req.body);
    if (!input) return sendError(res, 400, "Invalid order");
    const result = store.createOrder(input);
    if (!result) return sendError(res, 422, "Insufficient inventory or unknown product");
    return res.status(201).type("application/json").send(result);
  });

  app.get("/orders/:id", (req, res) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid order id");
    const order = store.getOrder(id);
    if (!order) return sendError(res, 404, "Order not found");
    return res.status(200).type("application/json").send(order);
  });

  app.patch("/orders/:id", (req, res) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid order id");
    const input = requestOrderUpdate(req.body);
    if (!input) return sendError(res, 400, "Invalid order");
    if (!store.updateOrder(id, input)) return sendError(res, 404, "Order not found");
    return res.status(200).type("text/plain").send("success");
  });

  app.delete("/orders/:id", (req, res) => {
    const id = parseInteger(req.params.id);
    if (id === undefined) return sendError(res, 400, "Invalid order id");
    if (!store.deleteOrder(id)) return sendError(res, 404, "Order not found");
    return res.status(200).type("text/plain").send("success");
  });

  app.use((err: unknown, _req: Request, res: Response, _next: express.NextFunction) => {
    const message = err instanceof Error ? err.message : "Invalid request";
    return sendError(res, 400, message);
  });

  return app;
}
