import express from "express";
import multer from "multer";
import { createStore } from "./store.js";
import {
  errorBody,
  isValidDate,
  isValidIntegerHeader,
  isValidProductType,
  isValidUuid,
  parseId,
  validateOrderBase,
  validateOrderUpdate,
  validateProductBase
} from "./validation.js";

const upload = multer({ storage: multer.memoryStorage() });

function sendError(res, status, error, message) {
  return res.status(status).json(errorBody(status, error, message));
}

function requireIntegerId(req, res) {
  const id = parseId(req.params.id);
  if (id === null) {
    sendError(res, 400, "Bad Request", "id must be an integer");
    return null;
  }
  return id;
}

export function createApp(store = createStore()) {
  const app = express();

  app.use(express.json());

  app.get("/products", (req, res) => {
    const { type } = req.query;
    const fromDate = req.query["from-date"];
    const toDate = req.query["to-date"];
    const pageSize = req.get("pageSize");

    if (!isValidProductType(type)) {
      return sendError(res, 400, "Bad Request", "type is not supported");
    }
    if (!isValidIntegerHeader(pageSize)) {
      return sendError(res, 400, "Bad Request", "pageSize must be an integer");
    }
    if ((fromDate && !isValidDate(fromDate)) || (toDate && !isValidDate(toDate))) {
      return sendError(res, 400, "Bad Request", "date filters must use yyyy-MM-dd");
    }

    return res.status(200).json(store.listProducts({ type, fromDate, toDate }));
  });

  app.post("/products", (req, res) => {
    if (!isValidUuid(req.get("Idempotency-Key"))) {
      return sendError(res, 400, "Bad Request", "Idempotency-Key header must be a UUID");
    }
    const validationError = validateProductBase(req.body);
    if (validationError) {
      return sendError(res, 400, "Bad Request", validationError);
    }
    return res.status(201).json({ id: store.createProduct(req.body) });
  });

  app.get("/products/:id", (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    const product = store.getProduct(id);
    if (!product) return sendError(res, 404, "Not Found", "Product not found");
    return res.status(200).json(product);
  });

  app.patch("/products/:id", (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    const validationError = validateProductBase(req.body);
    if (validationError) {
      return sendError(res, 400, "Bad Request", validationError);
    }
    if (!store.updateProduct(id, req.body)) {
      return sendError(res, 404, "Not Found", "Product not found");
    }
    return res.type("text/plain").status(200).send("success");
  });

  app.delete("/products/:id", (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    if (!store.deleteProduct(id)) {
      return sendError(res, 404, "Not Found", "Product not found");
    }
    return res.type("text/plain").status(200).send("success");
  });

  app.put("/products/:id/image", upload.single("image"), (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    if (!req.file) {
      return sendError(res, 400, "Bad Request", "image is required");
    }
    if (!store.setProductImage(id)) {
      return sendError(res, 404, "Not Found", "Product not found");
    }
    return res.status(200).json({ message: "Success" });
  });

  app.get("/orders", (_req, res) => {
    return res.status(200).json(store.listOrders());
  });

  app.post("/orders", (req, res) => {
    if (!isValidUuid(req.get("Idempotency-Key"))) {
      return sendError(res, 400, "Bad Request", "Idempotency-Key header must be a UUID");
    }
    const validationError = validateOrderBase(req.body);
    if (validationError) {
      return sendError(res, 400, "Bad Request", validationError);
    }
    if (!store.getProduct(req.body.productid)) {
      return sendError(res, 422, "Unprocessable Entity", "Product does not exist");
    }
    return res.status(201).json({ id: store.createOrder(req.body) });
  });

  app.get("/orders/:id", (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    const order = store.getOrder(id);
    if (!order) return sendError(res, 404, "Not Found", "Order not found");
    return res.status(200).json(order);
  });

  app.patch("/orders/:id", (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    const validationError = validateOrderUpdate(req.body);
    if (validationError) {
      return sendError(res, 400, "Bad Request", validationError);
    }
    if (!store.updateOrder(id, req.body)) {
      return sendError(res, 404, "Not Found", "Order not found");
    }
    return res.type("text/plain").status(200).send("success");
  });

  app.delete("/orders/:id", (req, res) => {
    const id = requireIntegerId(req, res);
    if (id === null) return undefined;
    if (!store.deleteOrder(id)) {
      return sendError(res, 404, "Not Found", "Order not found");
    }
    return res.type("text/plain").status(200).send("success");
  });

  app.use((err, _req, res, _next) => {
    if (err instanceof SyntaxError) {
      return sendError(res, 400, "Bad Request", "Request body is not valid JSON");
    }
    return sendError(res, 400, "Bad Request", err.message || "Bad request");
  });

  return app;
}
