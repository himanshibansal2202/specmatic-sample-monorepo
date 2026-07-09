import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { createBffClient } from "../src/api";
import { startSpecmaticBffMock } from "./specmaticMock";

let mock: Awaited<ReturnType<typeof startSpecmaticBffMock>>;

beforeAll(async () => {
  mock = await startSpecmaticBffMock();
});

afterAll(async () => {
  await mock?.stop();
});

describe("storefront workflows against the Specmatic BFF mock", () => {
  it("creates a product using the OpenAPI request body", async () => {
    const client = createBffClient(mock.baseUrl);

    await expect(client.createProduct({ name: "iPhone", type: "gadget", inventory: 100 })).resolves.toMatchObject({
      id: expect.any(Number)
    });
  });

  it("finds available products with the contract-declared query parameters and pageSize header", async () => {
    const client = createBffClient(mock.baseUrl);

    const products = await client.findAvailableProducts({
      type: "gadget",
      pageSize: 10,
      fromDate: "2025-01-01",
      toDate: "2025-11-28"
    });

    expect(products).toEqual([
      expect.objectContaining({
        id: expect.any(Number),
        name: expect.any(String),
        type: expect.stringMatching(/book|food|gadget|other/),
        inventory: expect.any(Number),
        createdOn: expect.any(String)
      })
    ]);
  });

  it("creates an order using the BFF order contract", async () => {
    const client = createBffClient(mock.baseUrl);

    await expect(client.createOrder({ productid: 1, count: 2 })).resolves.toMatchObject({
      id: expect.any(Number)
    });
  });
});
