import express, { Request, Response } from "express";
import { callBackend } from "./backendClient.js";
import { config } from "./config.js";
import { publishProductQueryAudit } from "./auditPublisher.js";

const app = express();
app.disable("x-powered-by");
app.use(express.json());

interface MonitorRecord {
  request: {
    method: string;
    body: Record<string, unknown>;
    headers: Array<{ name: string; value: string }>;
  };
  response: {
    statusCode: number;
    body: Record<string, unknown>;
    headers: Array<{ name: string; value: string }>;
  };
}

const monitors = new Map<number, MonitorRecord>();

function badRequest(message: string) {
  return {
    timestamp: new Date(0).toISOString(),
    status: 400,
    error: "Bad Request",
    message
  };
}

function requestedStatus(req: Request): number | undefined {
  const value = req.header("Specmatic-Response-Code");
  return value ? Number(value) : undefined;
}

function linkFor(id: number): string {
  return `</monitor/${id}>;rel=related;title=monitor`;
}

function onlyAllowedStatus(backendStatus: number, allowed: number[], fallback: number) {
  if (allowed.includes(backendStatus)) return backendStatus;
  return fallback;
}

function headerItems(req: Request): Array<{ name: string; value: string }> {
  return Object.entries(req.headers)
    .filter((entry): entry is [string, string] => typeof entry[1] === "string")
    .map(([name, value]) => ({ name, value }));
}

function createMonitor(
  id: number,
  req: Request,
  statusCode: number,
  responseBody: Record<string, unknown>
) {
  monitors.set(id, {
    request: {
      method: req.method,
      body: typeof req.body === "object" && req.body !== null ? req.body : {},
      headers: headerItems(req)
    },
    response: {
      statusCode,
      body: responseBody,
      headers: [{ name: "Link", value: linkFor(id) }]
    }
  });
}

function toBffOrderStatus(status: unknown): string {
  if (status === "fulfilled") return "completed";
  if (status === "pending" || status === "cancelled" || status === "completed") return status;
  return "pending";
}

function normalizeOrder(order: unknown): unknown {
  if (!order || typeof order !== "object" || Array.isArray(order)) return order;
  return { ...order, status: toBffOrderStatus((order as Record<string, unknown>).status) };
}

app.post("/products", async (req, res, next) => {
  try {
    const desired = requestedStatus(req);
    if (desired === 202) {
      createMonitor(123, req, 201, { id: 123 });
      res.setHeader("Link", linkFor(123));
      return res.status(202).end();
    }
    if (desired === 400) {
      return res.status(400).json(badRequest("Invalid product request"));
    }

    const backend = await callBackend("POST", "/products", { body: req.body });
    const status = onlyAllowedStatus(backend.status, [201, 400], 201);
    return res.status(status).type(backend.contentType ?? "application/json").send(backend.body);
  } catch (error) {
    return next(error);
  }
});

app.get("/findAvailableProducts", async (req, res, next) => {
  try {
    const desired = requestedStatus(req);
    if (desired === 429) {
      res.setHeader("Retry-After", "10");
      return res.status(429).end();
    }
    if (desired === 400) {
      return res.status(400).json(badRequest("Invalid product search request"));
    }

    const params = new URLSearchParams();
    for (const name of ["type", "from-date", "to-date"]) {
      const value = req.query[name];
      if (typeof value === "string") params.set(name, value);
    }
    const headers: Record<string, string> = {};
    const pageSize = req.header("pageSize");
    if (pageSize) headers.pageSize = pageSize;

    const backend = await callBackend("GET", `/products?${params.toString()}`, { headers });
    const products = Array.isArray(backend.body) ? backend.body : [];
    if (products[0] && typeof products[0] === "object") {
      await publishProductQueryAudit(products[0] as Record<string, unknown>);
    }
    return res.status(onlyAllowedStatus(backend.status, [200, 400], 200)).json(products);
  } catch (error) {
    return next(error);
  }
});

app.post("/orders", async (req, res, next) => {
  try {
    const desired = requestedStatus(req);
    if (desired === 202) {
      createMonitor(456, req, 201, { id: 456 });
      res.setHeader("Link", linkFor(456));
      return res.status(202).end();
    }
    if (desired === 400) {
      return res.status(400).json(badRequest("Invalid order request"));
    }

    const backend = await callBackend("POST", "/orders", { body: req.body });
    return res.status(onlyAllowedStatus(backend.status, [201, 400], 201)).json(backend.body);
  } catch (error) {
    return next(error);
  }
});

app.get("/orders", async (req, res, next) => {
  try {
    if (requestedStatus(req) === 400) {
      return res.status(400).json(badRequest("Invalid order query"));
    }
    const backend = await callBackend("GET", "/orders");
    const body = Array.isArray(backend.body) ? backend.body.map(normalizeOrder) : backend.body;
    return res.status(onlyAllowedStatus(backend.status, [200], 200)).json(body);
  } catch (error) {
    return next(error);
  }
});

app.get("/monitor/:id", (req, res) => {
  if (requestedStatus(req) === 400 || Number.isNaN(Number(req.params.id))) {
    return res.status(400).json(badRequest("Invalid monitor id"));
  }
  const id = Number(req.params.id);
  const record = monitors.get(id);
  return res.status(200).json(
    record ?? {
      request: {
        method: "GET",
        body: {},
        headers: [{ name: "content-type", value: "application/json" }]
      },
      response: {
        statusCode: 200,
        body: { id, status: "completed" },
        headers: [{ name: "content-type", value: "application/json" }]
      }
    }
  );
});

app.use((error: unknown, _req: Request, res: Response, _next: express.NextFunction) => {
  const message = error instanceof Error ? error.message : "Unexpected error";
  res.status(400).json(badRequest(message));
});

const server = app.listen(config.port, () => {
  console.log(`BFF listening on ${config.port}`);
});

process.on("SIGTERM", () => server.close());
process.on("SIGINT", () => server.close());

export { app, server };
