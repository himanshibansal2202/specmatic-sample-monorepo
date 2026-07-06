import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { GenericContainer, StartedTestContainer, Wait } from "testcontainers";
import { OrderBffClient } from "../src/api/orderBffClient";

const SPEC_IMAGE = "specmatic/enterprise:1.19.0";
const CONTAINER_MOCK_PORT = 8090;

describe("Order BFF contract consumption", () => {
  let container: StartedTestContainer;
  let client: OrderBffClient;

  beforeAll(async () => {
    const environment: Record<string, string> = {
      STUB_BASE_URL: `http://0.0.0.0:${CONTAINER_MOCK_PORT}`
    };

    if (process.env.SPECMATIC_LICENSE_KEY) {
      environment.SPECMATIC_LICENSE_KEY = process.env.SPECMATIC_LICENSE_KEY;
    }

    container = await new GenericContainer(SPEC_IMAGE)
      .withBindMounts([
        {
          source: process.cwd(),
          target: "/workspace",
          mode: "rw"
        }
      ])
      .withWorkingDir("/workspace")
      .withEnvironment(environment)
      .withCommand([
        "mock",
        "--config=specmatic.yaml",
        "--host=0.0.0.0",
        `--port=${CONTAINER_MOCK_PORT}`
      ])
      .withExposedPorts(CONTAINER_MOCK_PORT)
      .withWaitStrategy(Wait.forLogMessage("Mock server is running"))
      .start();

    client = new OrderBffClient(`http://${container.getHost()}:${container.getMappedPort(CONTAINER_MOCK_PORT)}`);
  });

  afterAll(async () => {
    if (container) {
      await container.exec(["sh", "-c", "kill -INT 1 || true"]);
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }
    await container?.stop();
  });

  it("creates a product from the ProductBase request body", async () => {
    await expect(
      client.createProduct({
        name: "iPhone",
        type: "gadget",
        inventory: 100
      })
    ).resolves.toEqual({ id: 1 });
  });

  it("finds available products with required date query parameters and pageSize header", async () => {
    await expect(
      client.findAvailableProducts({
        type: "gadget",
        pageSize: 10,
        fromDate: "2025-01-01",
        toDate: "2025-11-28"
      })
    ).resolves.toEqual([
      {
        name: "iPhone",
        id: 1,
        type: "gadget",
        inventory: 100,
        createdOn: "2024-01-01"
      }
    ]);
  });

  it("creates an order from the OrderBase request body", async () => {
    await expect(
      client.createOrder({
        productid: 1,
        count: 2
      })
    ).resolves.toEqual({ id: 1 });
  });

  it("retrieves orders", async () => {
    await expect(client.getOrders(100)).resolves.toEqual([
      {
        id: 1,
        productid: 1,
        count: 2,
        status: "completed"
      }
    ]);
  });

  it("retrieves monitor state", async () => {
    const response = await client.retrieveMonitor(123);
    expect(response).toHaveProperty("request");
    expect(response).toHaveProperty("response");
  });
});
