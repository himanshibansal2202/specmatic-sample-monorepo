import { describe, it, beforeAll, afterAll, expect } from "vitest";
import { execSync, spawn, type ChildProcess } from "child_process";
import { createProduct, findAvailableProducts, createOrder, getOrders, getMonitor } from "../src/api/bffClient.js";

const STUB_PORT = process.env.STUB_PORT || "8090";
const STUB_BASE_URL = `http://localhost:${STUB_PORT}`;
const CONTAINER_NAME = "specmatic-bff-stub";

let stubProcess: ChildProcess | null = null;

function waitForStub(url: string, timeoutMs = 60000): Promise<void> {
  const start = Date.now();
  return new Promise((resolve, reject) => {
    const check = async () => {
      try {
        const res = await fetch(url);
        if (res.ok || res.status < 500) return resolve();
      } catch {}
      if (Date.now() - start > timeoutMs) return reject(new Error("Stub startup timeout"));
      setTimeout(check, 500);
    };
    check();
  });
}

beforeAll(async () => {
  // Stop any existing container
  try { execSync(`docker rm -f ${CONTAINER_NAME}`, { stdio: "ignore" }); } catch {}

  // Start Specmatic stub via Docker
  stubProcess = spawn("docker", [
    "run", "--rm",
    "--name", CONTAINER_NAME,
    "-p", `${STUB_PORT}:9000`,
    "-v", `${process.cwd()}/specmatic.yaml:/usr/src/app/specmatic.yaml:ro`,
    "specmatic/specmatic",
    "stub", "--port=9000",
  ], { stdio: "pipe" });

  // Set env for the client
  process.env.BFF_BASE_URL = STUB_BASE_URL;

  await waitForStub(`${STUB_BASE_URL}/products`, 150000);
}, 200000);

afterAll(() => {
  try { execSync(`docker rm -f ${CONTAINER_NAME}`, { stdio: "ignore" }); } catch {}
});

describe("Frontend Contract Tests", () => {
  it("should create a product", async () => {
    const result = await createProduct({ name: "iPhone", type: "gadget", inventory: 100 });
    expect(result).toHaveProperty("id");
    expect(typeof result.id).toBe("number");
  });

  it("should find available products", async () => {
    const products = await findAvailableProducts({
      type: "gadget",
      pageSize: 10,
      fromDate: "2025-01-01",
      toDate: "2025-11-28",
    });
    expect(Array.isArray(products)).toBe(true);
    if (products.length > 0) {
      expect(products[0]).toHaveProperty("id");
      expect(products[0]).toHaveProperty("name");
      expect(products[0]).toHaveProperty("type");
      expect(products[0]).toHaveProperty("inventory");
    }
  });

  it("should create an order", async () => {
    const result = await createOrder({ productid: 1, count: 2 });
    expect(result).toHaveProperty("id");
    expect(typeof result.id).toBe("number");
  });

  it("should get orders", async () => {
    const orders = await getOrders(100);
    expect(Array.isArray(orders)).toBe(true);
    if (orders.length > 0) {
      expect(orders[0]).toHaveProperty("id");
      expect(orders[0]).toHaveProperty("productid");
      expect(orders[0]).toHaveProperty("count");
      expect(orders[0]).toHaveProperty("status");
    }
  });

  it("should get monitor status", async () => {
    const monitor = await getMonitor(1);
    expect(monitor).toHaveProperty("request");
    expect(monitor).toHaveProperty("response");
  });
});
