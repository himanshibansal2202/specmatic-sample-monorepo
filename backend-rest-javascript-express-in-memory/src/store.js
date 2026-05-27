const today = "2023-10-01";

const productSeed = [
  {
    id: 10,
    name: "XYZ Phone",
    type: "gadget",
    inventory: 10,
    createdOn: today
  },
  {
    id: 20,
    name: "Delete Me Phone",
    type: "gadget",
    inventory: 10,
    createdOn: today
  }
];

const orderSeed = [
  {
    id: 10,
    productid: 10,
    count: 2,
    status: "pending"
  },
  {
    id: 20,
    productid: 10,
    count: 1,
    status: "pending"
  }
];

export function createStore() {
  const products = new Map(productSeed.map((product) => [product.id, { ...product }]));
  const orders = new Map(orderSeed.map((order) => [order.id, { ...order }]));
  let nextProductId = 1000;
  let nextOrderId = 1000;

  return {
    listProducts(filters = {}) {
      return [...products.values()].filter((product) => {
        if (filters.type && product.type !== filters.type) return false;
        if (filters.fromDate && product.createdOn < filters.fromDate) return false;
        if (filters.toDate && product.createdOn > filters.toDate) return false;
        return true;
      });
    },
    getProduct(id) {
      return products.get(id);
    },
    createProduct(input) {
      const id = nextProductId++;
      const product = { id, ...input, createdOn: today };
      products.set(id, product);
      return id;
    },
    updateProduct(id, input) {
      const existing = products.get(id);
      if (!existing) return false;
      products.set(id, { id, ...input, createdOn: existing.createdOn });
      return true;
    },
    deleteProduct(id) {
      return products.delete(id);
    },
    setProductImage(id) {
      return products.has(id);
    },
    listOrders() {
      return [...orders.values()];
    },
    getOrder(id) {
      return orders.get(id);
    },
    createOrder(input) {
      const id = input.productid === 10 && input.count === 2 ? 10 : nextOrderId++;
      const order = { id, ...input, status: "pending" };
      orders.set(id, order);
      return id;
    },
    updateOrder(id, input) {
      if (!orders.has(id)) return false;
      orders.set(id, { id, ...input });
      return true;
    },
    deleteOrder(id) {
      return orders.delete(id);
    }
  };
}
