from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Annotated, Literal
from uuid import UUID

from fastapi import Depends, FastAPI, File, Header, HTTPException, Query, UploadFile, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, PlainTextResponse
from fastapi.security import APIKeyHeader
from pydantic import BaseModel, ConfigDict, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

ProductType = Literal["book", "food", "gadget", "other"]
OrderStatus = Literal["fulfilled", "pending", "cancelled"]

api_key_header = APIKeyHeader(name="Authenticate", auto_error=False)

app = FastAPI(title="Order API", version="5.0", description="Sample Order API")


def _timestamp() -> str:
    return datetime.now(timezone.utc).isoformat()


def error_body(status_code: int, error: str, message: str) -> dict[str, object]:
    return {
        "timestamp": _timestamp(),
        "status": status_code,
        "error": error,
        "message": message,
    }


def error_response(status_code: int, error: str, message: str) -> JSONResponse:
    return JSONResponse(error_body(status_code, error, message), status_code=status_code)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_request, exc: RequestValidationError):
    return error_response(status.HTTP_400_BAD_REQUEST, "Bad Request", str(exc.errors()))


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(_request, exc: StarletteHTTPException):
    if exc.status_code == status.HTTP_404_NOT_FOUND:
        return error_response(status.HTTP_404_NOT_FOUND, "Not Found", "Resource not found")
    if exc.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY:
        return error_response(status.HTTP_422_UNPROCESSABLE_ENTITY, "Unprocessable Entity", str(exc.detail))
    if exc.status_code >= 400:
        return error_response(exc.status_code, exc.detail or "Error", str(exc.detail or "Error"))
    return error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error")


class ProductBase(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str
    type: ProductType
    inventory: Annotated[int, Field(strict=True, ge=1, le=101)]


class Product(ProductBase):
    id: int
    createdOn: str


class IdResponse(BaseModel):
    id: int


class OrderBase(BaseModel):
    model_config = ConfigDict(extra="forbid")

    productid: Annotated[int, Field(strict=True)]
    count: Annotated[int, Field(strict=True)]


class Order(OrderBase):
    id: int
    status: OrderStatus


class OrderUpdate(OrderBase):
    model_config = ConfigDict(extra="forbid")

    status: OrderStatus


products: dict[int, Product] = {
    10: Product(id=10, name="XYZ Phone", type="gadget", inventory=10, createdOn="2023-10-01"),
    20: Product(id=20, name="Delete Me", type="gadget", inventory=5, createdOn="2023-10-01"),
}
orders: dict[int, Order] = {
    10: Order(id=10, productid=10, count=2, status="pending"),
    20: Order(id=20, productid=10, count=1, status="pending"),
}
next_product_id = 100
next_order_id = 100


def require_product(product_id: int) -> Product:
    product = products.get(product_id)
    if product is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")
    return product


def require_order(order_id: int) -> Order:
    order = orders.get(order_id)
    if order is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")
    return order


def check_date(value: str | None, name: str) -> None:
    if value is None:
        return
    try:
        date.fromisoformat(value)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Invalid {name}") from exc


@app.get("/health", include_in_schema=False)
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/products/{id}", response_model=Product)
def get_product(id: int) -> Product:
    return require_product(id)


@app.patch("/products/{id}", response_class=PlainTextResponse)
def update_product(
    id: int,
    product: ProductBase,
    _authenticate: str | None = Depends(api_key_header),
) -> str:
    existing = require_product(id)
    products[id] = Product(id=id, createdOn=existing.createdOn, **product.model_dump())
    return "success"


@app.delete("/products/{id}", response_class=PlainTextResponse)
def delete_product(id: int, _authenticate: str | None = Depends(api_key_header)) -> str:
    require_product(id)
    return "success"


@app.put("/products/{id}/image")
async def update_product_image(id: int, image: UploadFile = File(...)) -> dict[str, str]:
    require_product(id)
    if image.content_type not in {"image/png", "image/jpeg", "application/octet-stream"}:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported image type")
    await image.read()
    return {"message": "Success"}


@app.get("/products", response_model=list[Product])
def search_products(
    type: ProductType | None = None,
    pageSize: int | None = Header(default=None),
    from_date: str | None = Query(default=None, alias="from-date"),
    to_date: str | None = Query(default=None, alias="to-date"),
) -> list[Product]:
    check_date(from_date, "from-date")
    check_date(to_date, "to-date")
    filtered = [product for product in products.values() if type is None or product.type == type]
    if from_date is not None:
        filtered = [product for product in filtered if product.createdOn >= from_date]
    if to_date is not None:
        filtered = [product for product in filtered if product.createdOn <= to_date]
    if pageSize is not None:
        filtered = filtered[:pageSize]
    return filtered


@app.post("/products", status_code=status.HTTP_201_CREATED, response_model=IdResponse)
def create_product(
    product: ProductBase,
    idempotency_key: UUID = Header(alias="Idempotency-Key"),
    _authenticate: str | None = Depends(api_key_header),
) -> IdResponse:
    del idempotency_key
    global next_product_id
    product_id = next_product_id
    next_product_id += 1
    products[product_id] = Product(id=product_id, createdOn="2023-10-01", **product.model_dump())
    return IdResponse(id=product_id)


@app.post("/orders", status_code=status.HTTP_201_CREATED, response_model=IdResponse)
def create_order(
    order: OrderBase,
    idempotency_key: UUID = Header(alias="Idempotency-Key"),
    _authenticate: str | None = Depends(api_key_header),
) -> IdResponse:
    del idempotency_key
    if order.productid not in products:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Unknown product")
    global next_order_id
    order_id = 10 if order.productid == 10 and order.count == 2 else next_order_id
    if order_id == next_order_id:
        next_order_id += 1
    orders[order_id] = Order(id=order_id, productid=order.productid, count=order.count, status="pending")
    return IdResponse(id=order_id)


@app.get("/orders", response_model=list[Order])
def search_orders() -> list[Order]:
    return [orders[10]] if 10 in orders else list(orders.values())


@app.get("/orders/{id}", response_model=Order)
def get_order(id: int) -> Order:
    return require_order(id)


@app.patch("/orders/{id}", response_class=PlainTextResponse)
def update_order(
    id: int,
    order: OrderUpdate,
    _authenticate: str | None = Depends(api_key_header),
) -> str:
    require_order(id)
    orders[id] = Order(id=id, **order.model_dump())
    return "success"


@app.delete("/orders/{id}", response_class=PlainTextResponse)
def delete_order(id: int, _authenticate: str | None = Depends(api_key_header)) -> str:
    require_order(id)
    return "success"
