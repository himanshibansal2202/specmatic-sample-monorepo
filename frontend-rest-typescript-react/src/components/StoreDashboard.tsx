import { useMemo, useState } from "react";
import { OrderBffClient, Product, ProductType } from "../api/orderBffClient";

const productTypes: ProductType[] = ["book", "food", "gadget", "other"];

interface StoreDashboardProps {
  bffBaseUrl: string;
}

export function StoreDashboard({ bffBaseUrl }: StoreDashboardProps) {
  const client = useMemo(() => new OrderBffClient(bffBaseUrl), [bffBaseUrl]);
  const [products, setProducts] = useState<Product[]>([]);
  const [productType, setProductType] = useState<ProductType>("gadget");
  const [message, setMessage] = useState("Ready");

  async function createProduct() {
    const result = await client.createProduct({
      name: "iPhone",
      type: productType,
      inventory: 100
    });
    setMessage(`Created product ${result?.id ?? "accepted"}`);
  }

  async function findProducts() {
    const result = await client.findAvailableProducts({
      type: productType,
      pageSize: 10,
      fromDate: "2025-01-01",
      toDate: "2025-11-28"
    });
    setProducts(result);
    setMessage(`Loaded ${result.length} product${result.length === 1 ? "" : "s"}`);
  }

  async function createOrder() {
    const result = await client.createOrder({
      productid: 1,
      count: 2
    });
    setMessage(`Created order ${result?.id ?? "accepted"}`);
  }

  return (
    <main className="app-shell">
      <section className="toolbar" aria-label="Store controls">
        <label>
          Product type
          <select value={productType} onChange={(event) => setProductType(event.target.value as ProductType)}>
            {productTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </label>
        <button type="button" onClick={createProduct}>Create Product</button>
        <button type="button" onClick={findProducts}>Find Available</button>
        <button type="button" onClick={createOrder}>Create Order</button>
      </section>

      <section className="status" aria-live="polite">
        <span>BFF</span>
        <strong>{bffBaseUrl}</strong>
        <p>{message}</p>
      </section>

      <section className="product-list" aria-label="Available products">
        {products.map((product) => (
          <article key={product.id} className="product-row">
            <div>
              <strong>{product.name}</strong>
              <span>{product.type}</span>
            </div>
            <span>{product.inventory} in stock</span>
            <span>Created {product.createdOn}</span>
          </article>
        ))}
      </section>
    </main>
  );
}
