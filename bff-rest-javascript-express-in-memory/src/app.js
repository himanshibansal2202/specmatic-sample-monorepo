import express from "express";
import { BackendClient } from "./backend-client.js";
import { getConfig } from "./config.js";

function badRequest(message) {
  return {
    timestamp: new Date(0).toISOString(),
    status: 400,
    error: "Bad Request",
    message
  };
}

const productTypes = new Set(["book", "food", "gadget", "other"]);
const datePattern = /^\d{4}-\d{2}-\d{2}$/;

function isInteger(value) {
  return typeof value === "number" && Number.isInteger(value);
}

function isDateString(value) {
  if (typeof value !== "string" || !datePattern.test(value)) {
    return false;
  }
  const date = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(date.valueOf()) && date.toISOString().slice(0, 10) === value;
}

function isIntegerString(value) {
  return typeof value === "string" && /^-?\d+$/.test(value);
}

function validateProductBase(body) {
  return body &&
    typeof body === "object" &&
    typeof body.name === "string" &&
    productTypes.has(body.type) &&
    isInteger(body.inventory) &&
    body.inventory >= 1 &&
    body.inventory <= 101;
}

function validateOrderBase(body) {
  return body &&
    typeof body === "object" &&
    isInteger(body.productid) &&
    isInteger(body.count);
}

function validateFindProducts(req) {
  const { type } = req.query;
  const fromDate = req.query["from-date"];
  const toDate = req.query["to-date"];
  const pageSize = req.headers.pagesize;

  if (type !== undefined && !productTypes.has(type)) {
    return false;
  }

  return isDateString(fromDate) && isDateString(toDate) && isIntegerString(pageSize);
}

function sendDependencyResult(res, result, transform = (body) => body) {
  if (result.contentType && !result.contentType.includes("application/json")) {
    res.type(result.contentType);
  }
  res.status(result.status).send(transform(result.body));
}

function wantsAccepted(req) {
  return req.headers["specmatic-response-code"] === "202";
}

function wantsTooManyRequests(req) {
  return req.headers["specmatic-response-code"] === "429";
}

function headerItems(headers) {
  return Object.entries(headers)
    .filter(([_name, value]) => value !== undefined)
    .map(([name, value]) => ({ name, value: String(value) }));
}

function sendAccepted(req, res, monitors) {
  const id = monitors.nextId++;
  monitors.items.set(id, {
    request: {
      method: req.method,
      body: req.body || {},
      headers: headerItems(req.headers)
    },
    response: {
      statusCode: 201,
      body: { id },
      headers: []
    }
  });

  res.status(202).set("Link", `</monitor/${id}>;rel=related;title=monitor`).end();
}

function toBffOrder(order) {
  if (!order || typeof order !== "object") {
    return order;
  }

  return {
    ...order,
    status: order.status === "fulfilled" ? "completed" : order.status
  };
}

export function createApp(config = getConfig()) {
  const app = express();
  const backendClient = new BackendClient(config);
  const monitors = {
    nextId: 1,
    items: new Map()
  };

  app.use(express.json());

  app.get("/health", (_req, res) => {
    res.status(200).json({ status: "ok" });
  });

  app.post("/products", async (req, res, next) => {
    try {
      if (!validateProductBase(req.body)) {
        res.status(400).json(badRequest("Product request does not match the contract"));
        return;
      }

      if (wantsAccepted(req)) {
        sendAccepted(req, res, monitors);
        return;
      }

      const result = await backendClient.createProduct(req.body, req.headers);
      sendDependencyResult(res, result);
    } catch (error) {
      next(error);
    }
  });

  app.get("/findAvailableProducts", async (req, res, next) => {
    try {
      if (wantsTooManyRequests(req)) {
        res.status(429).end();
        return;
      }

      if (!validateFindProducts(req)) {
        res.status(400).json(badRequest("Product search parameters do not match the contract"));
        return;
      }

      const result = await backendClient.findAvailableProducts(req.query, req.headers);
      sendDependencyResult(res, result);
    } catch (error) {
      next(error);
    }
  });

  app.post("/orders", async (req, res, next) => {
    try {
      if (!validateOrderBase(req.body)) {
        res.status(400).json(badRequest("Order request does not match the contract"));
        return;
      }

      if (wantsAccepted(req)) {
        sendAccepted(req, res, monitors);
        return;
      }

      const result = await backendClient.createOrder(req.body, req.headers);
      sendDependencyResult(res, result);
    } catch (error) {
      next(error);
    }
  });

  app.get("/orders", async (req, res, next) => {
    try {
      if (req.query.orderId !== undefined && !isIntegerString(req.query.orderId)) {
        res.status(200).json([]);
        return;
      }

      const result = await backendClient.getOrders(req.query.orderId);
      sendDependencyResult(res, result, (body) => {
        const orders = Array.isArray(body) ? body : [body];
        return orders.map(toBffOrder);
      });
    } catch (error) {
      next(error);
    }
  });

  app.get("/monitor/:id", (req, res) => {
    const id = Number(req.params.id);
    if (!Number.isInteger(id)) {
      res.status(400).json(badRequest("Monitor id must be an integer"));
      return;
    }

    res.status(200).json(monitors.items.get(id) || {
      request: {
        method: "GET",
        body: {},
        headers: []
      },
      response: {
        statusCode: 202,
        body: { id },
        headers: [
          {
            name: "Link",
            value: `</monitor/${id}>;rel=related;title=monitor`
          }
        ]
      }
    });
  });

  app.use((err, _req, res, _next) => {
    if (err instanceof SyntaxError && "body" in err) {
      res.status(400).json(badRequest("Malformed JSON request body"));
      return;
    }

    res.status(400).json(badRequest(err.message || "Request could not be processed"));
  });

  return app;
}
