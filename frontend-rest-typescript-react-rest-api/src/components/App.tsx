import React, { useEffect, useState } from "react";
import { findAvailableProducts, createProduct, createOrder } from "../api/bffClient.js";
import type { Product } from "../types/index.js";

export default function App() {
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    findAvailableProducts({ pageSize: 10, fromDate: "2025-01-01", toDate: "2025-12-31" })
      .then(setProducts)
      .catch(console.error);
  }, []);

  const handleCreateProduct = async () => {
    await createProduct({ name: "Sample", type: "gadget", inventory: 10 });
  };

  const handleCreateOrder = async () => {
    if (products.length > 0) {
      await createOrder({ productid: products[0].id, count: 1 });
    }
  };

  return (
    <div>
      <h1>Product Store</h1>
      <button onClick={handleCreateProduct}>Create Product</button>
      <button onClick={handleCreateOrder}>Create Order</button>
      <ul>
        {products.map((p) => (
          <li key={p.id}>{p.name} ({p.type}) - {p.inventory} in stock</li>
        ))}
      </ul>
    </div>
  );
}
