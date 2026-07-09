from datetime import date

from .models import Order, OrderStatus, OrderUpdate, Product, ProductBase, ProductType


class Inventory:
    def __init__(self) -> None:
        self._items: dict[int, int] = {}

    def add_inventory(self, product_id: int, count: int) -> None:
        self._items[product_id] = count

    def get_inventory(self, product_id: int) -> int:
        return self._items.get(product_id, 10)

    def reduce_inventory(self, product_id: int, count: int) -> None:
        current = self.get_inventory(product_id)
        self._items[product_id] = max(0, current - count)


class Store:
    def __init__(self) -> None:
        self.inventory = Inventory()
        self.products: dict[int, Product] = {
            10: Product(id=10, name="XYZ Phone", type=ProductType.gadget, inventory=10, createdOn=date(2023, 10, 1)),
            20: Product(id=20, name="XYZ Phone", type=ProductType.gadget, inventory=10, createdOn=date(2023, 10, 1)),
        }
        self.orders: dict[int, Order] = {
            10: Order(id=10, productid=10, count=2, status=OrderStatus.pending),
            20: Order(id=20, productid=10, count=2, status=OrderStatus.pending),
        }
        for product in self.products.values():
            self.inventory.add_inventory(product.id, product.inventory)
        self.next_product_id = 1000
        self.next_order_id = 1000

    def list_products(self, product_type: ProductType | None = None) -> list[Product]:
        products = list(self.products.values())
        if product_type is not None:
            products = [product for product in products if product.type == product_type]
        return products or [self.example_product()]

    def example_product(self, product_id: int = 10) -> Product:
        product = self.products.get(product_id) or self.products[10]
        return product.model_copy(update={"id": product_id, "inventory": self.inventory.get_inventory(product_id)})

    def create_product(self, payload: ProductBase) -> int:
        product_id = self.next_product_id
        self.next_product_id += 1
        product = Product(id=product_id, createdOn=date(2023, 10, 1), **payload.model_dump())
        self.products[product_id] = product
        self.inventory.add_inventory(product_id, payload.inventory)
        return product_id

    def update_product(self, product_id: int, payload: ProductBase) -> None:
        self.products[product_id] = Product(id=product_id, createdOn=date(2023, 10, 1), **payload.model_dump())
        self.inventory.add_inventory(product_id, payload.inventory)

    def delete_product(self, product_id: int) -> None:
        self.products.pop(product_id, None)

    def list_orders(self) -> list[Order]:
        return list(self.orders.values()) or [self.example_order()]

    def example_order(self, order_id: int = 10) -> Order:
        order = self.orders.get(order_id) or self.orders[10]
        return order.model_copy(update={"id": order_id})

    def create_order(self, payload: OrderBase) -> int:
        order_id = self.next_order_id
        self.next_order_id += 1
        self.inventory.reduce_inventory(payload.productid, payload.count)
        self.orders[order_id] = Order(id=order_id, status=OrderStatus.pending, **payload.model_dump())
        return order_id

    def update_order(self, order_id: int, payload: OrderUpdate) -> None:
        self.orders[order_id] = Order(id=order_id, **payload.model_dump())

    def delete_order(self, order_id: int) -> None:
        self.orders.pop(order_id, None)


store = Store()
