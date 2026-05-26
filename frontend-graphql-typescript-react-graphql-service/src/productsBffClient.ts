export type ProductType = "gadget" | "book" | "food" | "other";

export interface Product {
  id: string;
  name: string;
  inventory: number;
  type: ProductType;
}

export interface Offer {
  offerCode: string;
  validUntil: string;
}

export interface NewProductInput {
  name: string;
  inventory: number;
  type: ProductType;
}

export interface ProductsBffClientConfig {
  graphqlUrl: string;
  region: string;
  fetchImpl?: typeof fetch;
}

interface GraphqlResponse<T> {
  data?: T;
  errors?: Array<{ message: string }>;
}

export class ProductsBffClient {
  private readonly fetchImpl: typeof fetch;

  constructor(private readonly clientConfig: ProductsBffClientConfig) {
    this.fetchImpl = clientConfig.fetchImpl ?? fetch;
  }

  async findAvailableProducts(type: ProductType, pageSize = 10): Promise<Product[]> {
    const response = await this.request<{ findAvailableProducts: Product[] }>(
      `query {
        findAvailableProducts(type: ${type}, pageSize: ${pageSize}) { id name inventory type }
      }`,
      { "X-region": this.clientConfig.region }
    );

    return response.findAvailableProducts;
  }

  async findOffersForDate(date: string): Promise<Offer[]> {
    const response = await this.request<{ findOffersForDate: Offer[] }>(
      `query {
        findOffersForDate(date: "${escapeGraphqlString(date)}") { offerCode validUntil }
      }`
    );

    return response.findOffersForDate;
  }

  async createProduct(newProduct: NewProductInput): Promise<Product> {
    const response = await this.request<{ createProduct: Product }>(
      `mutation {
        createProduct(newProduct: {
          name: "${escapeGraphqlString(newProduct.name)}",
          inventory: ${newProduct.inventory},
          type: ${newProduct.type}
        }) { id name inventory type }
      }`
    );

    return response.createProduct;
  }

  private async request<T>(
    query: string,
    extraHeaders: Record<string, string> = {}
  ): Promise<T> {
    const response = await this.fetchImpl(this.clientConfig.graphqlUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...extraHeaders
      },
      body: JSON.stringify({ query })
    });

    const payload = (await response.json()) as GraphqlResponse<T>;

    if (!response.ok || payload.errors?.length) {
      const message = payload.errors?.map((error) => error.message).join("; ") || response.statusText;
      throw new Error(`Products BFF request failed: ${message}`);
    }

    if (!payload.data) {
      throw new Error("Products BFF response did not contain data");
    }

    return payload.data;
  }
}

function escapeGraphqlString(value: string): string {
  return value.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
}
