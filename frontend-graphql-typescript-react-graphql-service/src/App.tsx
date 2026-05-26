import { FormEvent, useMemo, useState } from "react";
import { config } from "./config";
import { NewProductInput, Product, ProductType, ProductsBffClient, Offer } from "./productsBffClient";
import "./styles.css";

const productTypes: ProductType[] = ["gadget", "book", "food", "other"];

export function App() {
  const client = useMemo(
    () => new ProductsBffClient({ graphqlUrl: config.bffGraphqlUrl, region: config.region }),
    []
  );
  const [products, setProducts] = useState<Product[]>([]);
  const [offers, setOffers] = useState<Offer[]>([]);
  const [createdProduct, setCreatedProduct] = useState<Product | null>(null);
  const [selectedType, setSelectedType] = useState<ProductType>("gadget");
  const [pageSize, setPageSize] = useState(10);
  const [offerDate, setOfferDate] = useState("2024-12-31");
  const [newProduct, setNewProduct] = useState<NewProductInput>({
    name: "The Almanac",
    inventory: 10,
    type: "book"
  });
  const [status, setStatus] = useState("Ready");

  async function loadProducts() {
    setStatus("Loading products");
    setProducts(await client.findAvailableProducts(selectedType, pageSize));
    setStatus("Products loaded");
  }

  async function loadOffers() {
    setStatus("Loading offers");
    setOffers(await client.findOffersForDate(offerDate));
    setStatus("Offers loaded");
  }

  async function submitProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus("Creating product");
    setCreatedProduct(await client.createProduct(newProduct));
    setStatus("Product created");
  }

  return (
    <main className="app-shell">
      <section className="workspace">
        <header className="masthead">
          <div>
            <p className="eyebrow">Specmatic GraphQL frontend</p>
            <h1>Products cockpit</h1>
          </div>
          <span className="status-pill">{status}</span>
        </header>

        <div className="grid">
          <section className="panel">
            <h2>Find available products</h2>
            <div className="controls-row">
              <label>
                Type
                <select value={selectedType} onChange={(event) => setSelectedType(event.target.value as ProductType)}>
                  {productTypes.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Page size
                <input
                  min="1"
                  max="50"
                  type="number"
                  value={pageSize}
                  onChange={(event) => setPageSize(Number(event.target.value))}
                />
              </label>
              <button type="button" onClick={loadProducts}>
                Search
              </button>
            </div>
            <ResultTable products={products} />
          </section>

          <section className="panel">
            <h2>Create product</h2>
            <form className="stack" onSubmit={submitProduct}>
              <label>
                Name
                <input
                  value={newProduct.name}
                  onChange={(event) => setNewProduct({ ...newProduct, name: event.target.value })}
                />
              </label>
              <div className="controls-row">
                <label>
                  Inventory
                  <input
                    type="number"
                    value={newProduct.inventory}
                    onChange={(event) => setNewProduct({ ...newProduct, inventory: Number(event.target.value) })}
                  />
                </label>
                <label>
                  Type
                  <select
                    value={newProduct.type}
                    onChange={(event) => setNewProduct({ ...newProduct, type: event.target.value as ProductType })}
                  >
                    {productTypes.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <button type="submit">Create</button>
            </form>
            {createdProduct ? (
              <pre className="json-output">{JSON.stringify(createdProduct, null, 2)}</pre>
            ) : null}
          </section>

          <section className="panel wide">
            <h2>Find offers</h2>
            <div className="controls-row">
              <label>
                Date
                <input value={offerDate} onChange={(event) => setOfferDate(event.target.value)} />
              </label>
              <button type="button" onClick={loadOffers}>
                Load offers
              </button>
            </div>
            <div className="offer-list">
              {offers.map((offer) => (
                <article className="offer" key={offer.offerCode}>
                  <strong>{offer.offerCode}</strong>
                  <span>{offer.validUntil}</span>
                </article>
              ))}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}

function ResultTable({ products }: { products: Product[] }) {
  if (products.length === 0) {
    return <p className="empty-state">No products loaded.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Inventory</th>
          <th>Type</th>
        </tr>
      </thead>
      <tbody>
        {products.map((product) => (
          <tr key={product.id}>
            <td>{product.id}</td>
            <td>{product.name}</td>
            <td>{product.inventory}</td>
            <td>{product.type}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
