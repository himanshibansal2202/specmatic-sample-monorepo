import { ClipboardCheck, PackagePlus, Search, ShoppingCart } from "lucide-react";
import { ChangeEvent, FormEvent, useMemo, useState } from "react";
import { BffApiError, Product, ProductType, createBffClient } from "./api";
import { config } from "./config";
import "./styles.css";

const productTypes: ProductType[] = ["book", "food", "gadget", "other"];

function inputValue(event: ChangeEvent<HTMLInputElement>): string {
  return (event.currentTarget as unknown as HTMLInputElement & { value: string }).value;
}

function selectValue(event: ChangeEvent<HTMLSelectElement>): string {
  return (event.currentTarget as unknown as HTMLSelectElement & { value: string }).value;
}

export default function App() {
  const client = useMemo(() => createBffClient(config.bffBaseUrl), []);
  const [products, setProducts] = useState<Product[]>([]);
  const [productName, setProductName] = useState("iPhone");
  const [productType, setProductType] = useState<ProductType>("gadget");
  const [inventory, setInventory] = useState(100);
  const [productId, setProductId] = useState(1);
  const [orderCount, setOrderCount] = useState(2);
  const [message, setMessage] = useState("Ready to call the BFF mock");
  const [busy, setBusy] = useState(false);

  async function run(action: () => Promise<string>) {
    setBusy(true);
    try {
      setMessage(await action());
    } catch (error) {
      if (error instanceof BffApiError) {
        setMessage(`BFF returned HTTP ${error.status}`);
      } else {
        setMessage(error instanceof Error ? error.message : "Unexpected error");
      }
    } finally {
      setBusy(false);
    }
  }

  function createProduct(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      const response = await client.createProduct({ name: productName, type: productType, inventory });
      return `Product accepted with id ${response.id}`;
    });
  }

  function findProducts() {
    void run(async () => {
      const response = await client.findAvailableProducts({
        type: productType,
        pageSize: 10,
        fromDate: "2025-01-01",
        toDate: "2025-11-28"
      });
      setProducts(response);
      return `Loaded ${response.length} available product${response.length === 1 ? "" : "s"}`;
    });
  }

  function createOrder(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      const response = await client.createOrder({ productid: productId, count: orderCount });
      return `Order accepted with id ${response.id}`;
    });
  }

  return (
    <main className="app-shell">
      <section className="top-band">
        <div>
          <p className="eyebrow">Specmatic Store</p>
          <h1>Contract verified storefront</h1>
          <p className="summary">
            A React client for product search and ordering workflows backed by a Specmatic-generated BFF mock.
          </p>
        </div>
        <div className="status" role="status" aria-live="polite">
          <ClipboardCheck size={20} aria-hidden="true" />
          <span>{message}</span>
        </div>
      </section>

      <section className="workspace" aria-busy={busy}>
        <form className="panel" onSubmit={createProduct}>
          <div className="panel-title">
            <PackagePlus size={20} aria-hidden="true" />
            <h2>Create Product</h2>
          </div>
          <label>
            Name
            <input value={productName} onChange={(event) => setProductName(inputValue(event))} />
          </label>
          <label>
            Type
            <select value={productType} onChange={(event) => setProductType(selectValue(event) as ProductType)}>
              {productTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>
          <label>
            Inventory
            <input type="number" min={1} max={101} value={inventory} onChange={(event) => setInventory(Number(inputValue(event)))} />
          </label>
          <button type="submit" disabled={busy}>Create</button>
        </form>

        <section className="panel">
          <div className="panel-title">
            <Search size={20} aria-hidden="true" />
            <h2>Find Available Products</h2>
          </div>
          <button type="button" onClick={findProducts} disabled={busy}>Search</button>
          <div className="result-list" aria-label="Available products">
            {products.map((product) => (
              <article key={product.id} className="product-row">
                <strong>{product.name}</strong>
                <span>{product.type}</span>
                <span>{product.inventory} in stock</span>
                <span>{product.createdOn}</span>
              </article>
            ))}
          </div>
        </section>

        <form className="panel" onSubmit={createOrder}>
          <div className="panel-title">
            <ShoppingCart size={20} aria-hidden="true" />
            <h2>Create Order</h2>
          </div>
          <label>
            Product ID
            <input type="number" min={1} value={productId} onChange={(event) => setProductId(Number(inputValue(event)))} />
          </label>
          <label>
            Count
            <input type="number" min={1} value={orderCount} onChange={(event) => setOrderCount(Number(inputValue(event)))} />
          </label>
          <button type="submit" disabled={busy}>Order</button>
        </form>
      </section>
    </main>
  );
}
