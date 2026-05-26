import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { ProductsBffClient } from "../src/productsBffClient";
import { startSpecmaticMock, type SpecmaticMock } from "./specmaticMock";

describe("Products BFF GraphQL contract", () => {
  let mock: SpecmaticMock;
  let client: ProductsBffClient;

  beforeAll(async () => {
    mock = await startSpecmaticMock();
    client = new ProductsBffClient({
      graphqlUrl: mock.endpoint,
      region: process.env.VITE_REGION ?? "north-west"
    });
  }, 150_000);

  afterAll(async () => {
    await mock?.stop();
  });

  it("finds available products with the required region header", async () => {
    await expect(client.findAvailableProducts("gadget", 10)).resolves.toEqual([
      {
        id: "10",
        name: "The Almanac",
        inventory: 10,
        type: "book"
      },
      {
        id: "20",
        name: "iPhone",
        inventory: 15,
        type: "gadget"
      }
    ]);
  });

  it("creates a product", async () => {
    await expect(
      client.createProduct({
        name: "The Almanac",
        inventory: 10,
        type: "book"
      })
    ).resolves.toEqual({
      id: "10",
      name: "The Almanac",
      inventory: 10,
      type: "book"
    });
  });

  it("finds offers for a date", async () => {
    await expect(client.findOffersForDate("2024-12-31")).resolves.toEqual([
      {
        offerCode: "WKND30",
        validUntil: "2024-12-12"
      },
      {
        offerCode: "SUNDAY20",
        validUntil: "2024-12-25"
      }
    ]);
  });
});
