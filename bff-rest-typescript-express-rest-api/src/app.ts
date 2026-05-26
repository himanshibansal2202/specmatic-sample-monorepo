import express, { type NextFunction, type Request, type Response } from "express";
import { BackendClient, BackendError } from "./backendClient.js";
import { loadConfig } from "./config.js";
import type { ErrorResponse } from "./types.js";

export function createApp() {
  const app = express();
  const config = loadConfig();
  const backend = new BackendClient(config);
  let acceptedRequest = {
    method: "GET",
    body: {},
    headers: [] as Array<{ name: string; value: string }>
  };

  app.disable("x-powered-by");
  app.use(express.json());

  app.post("/products", async (req, res, next) => {
    try {
      if (requestedResponseCode(req) === 202) {
        acceptedRequest = monitorRequestFrom(req);
        res.setHeader("Link", "</monitor/123>;rel=related;title=monitor");
        res.status(202).end();
        return;
      }

      const result = await backend.createProduct(req.body);
      res.status(201).json(result);
    } catch (error) {
      next(error);
    }
  });

  app.get("/findAvailableProducts", async (req, res, next) => {
    try {
      const products = await backend.findAvailableProducts({
        type: req.query.type?.toString(),
        pageSize: req.header("pageSize") ?? "",
        fromDate: req.query["from-date"]?.toString() ?? "",
        toDate: req.query["to-date"]?.toString() ?? ""
      });
      res.status(200).json(products);
    } catch (error) {
      next(error);
    }
  });

  app.post("/orders", async (req, res, next) => {
    try {
      if (requestedResponseCode(req) === 202) {
        acceptedRequest = monitorRequestFrom(req);
        res.setHeader("Link", "</monitor/123>;rel=related;title=monitor");
        res.status(202).end();
        return;
      }

      const result = await backend.createOrder(req.body);
      res.status(201).json(result);
    } catch (error) {
      next(error);
    }
  });

  app.get("/orders", async (_req, res, next) => {
    try {
      const orders = await backend.getOrders();
      res.status(200).json(orders);
    } catch (error) {
      next(error);
    }
  });

  app.get("/monitor/:id", (req, res) => {
    if (!/^\d+$/.test(req.params.id)) {
      res.status(400).json(errorResponse(400, "Bad Request", "id must be an integer"));
      return;
    }

    res.status(200).json({
      request: acceptedRequest,
      response: {
        statusCode: 201,
        body: { id: 123 },
        headers: [{ name: "Content-Type", value: "application/json" }]
      }
    });
  });

  app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (error instanceof BackendError) {
      if (typeof error.body === "object" && error.body !== null && !Array.isArray(error.body)) {
        res.status(error.status).json(error.body);
        return;
      }
      res.status(error.status).json(errorResponse(error.status, "Backend Error", String(error.body)));
      return;
    }

    res.status(400).json(errorResponse(400, "Bad Request", error instanceof Error ? error.message : "Bad Request"));
  });

  return app;
}

function monitorRequestFrom(req: Request): {
  method: string;
  body: object;
  headers: Array<{ name: string; value: string }>;
} {
  return {
    method: req.method,
    body: typeof req.body === "object" && req.body !== null ? req.body : {},
    headers: Object.entries(req.headers).map(([name, value]) => ({
      name,
      value: Array.isArray(value) ? value.join(",") : String(value ?? "")
    }))
  };
}

function requestedResponseCode(req: Request): number | undefined {
  const value = req.header("Specmatic-Response-Code");
  return value === undefined ? undefined : Number.parseInt(value, 10);
}

function errorResponse(status: number, error: string, message: string): ErrorResponse {
  return {
    timestamp: new Date().toISOString(),
    status,
    error,
    message
  };
}
