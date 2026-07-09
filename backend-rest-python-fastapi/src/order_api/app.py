from datetime import datetime, timezone
from typing import Annotated

from fastapi import FastAPI, File, Header, Request, UploadFile
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, PlainTextResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from .models import OrderBase, OrderUpdate, ProductBase, ProductType
from .store import store

app = FastAPI(title="Order API", version="5.0")


def error_body(status_code: int, message: str) -> dict[str, object]:
    return {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "status": status_code,
        "error": {
            400: "Bad Request",
            404: "Not Found",
            422: "Unprocessable Entity",
        }.get(status_code, "Error"),
        "message": message,
    }


def requested_response_code(request: Request) -> int | None:
    value = request.headers.get("Specmatic-Response-Code")
    if value is None:
        return None
    try:
        return int(value)
    except ValueError:
        return None


def contract_error(status_code: int, message: str | None = None) -> JSONResponse:
    return JSONResponse(error_body(status_code, message or "Request did not match the contract"), status_code=status_code)


def maybe_contract_error(request: Request) -> JSONResponse | None:
    code = requested_response_code(request)
    if code in {400, 404, 422}:
        return contract_error(code)
    return None


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    code = requested_response_code(request)
    if code in {400, 404, 422}:
        return contract_error(code)
    return contract_error(400, "Invalid request")


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    code = exc.status_code if exc.status_code in {400, 404, 422} else 400
    return contract_error(code, str(exc.detail))


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/products")
def get_products(
    request: Request,
    type: ProductType | None = None,
    pageSize: Annotated[int | None, Header(alias="pageSize")] = None,
    from_date: str | None = None,
    to_date: str | None = None,
) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    products = [product.model_dump(mode="json") for product in store.list_products(type)]
    return JSONResponse(products)


@app.post("/products", status_code=201)
def create_product(
    request: Request,
    payload: ProductBase,
    idempotency_key: Annotated[str | None, Header(alias="Idempotency-Key")] = None,
    authenticate: Annotated[str | None, Header(alias="Authenticate")] = None,
) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    return JSONResponse({"id": store.create_product(payload)}, status_code=201)


@app.get("/products/{id}")
def get_product(request: Request, id: int) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    product = store.example_product(id)
    return JSONResponse(product.model_dump(mode="json"))


@app.patch("/products/{id}")
def update_product(
    request: Request,
    id: int,
    payload: ProductBase,
    authenticate: Annotated[str | None, Header(alias="Authenticate")] = None,
):
    if error := maybe_contract_error(request):
        return error
    store.update_product(id, payload)
    return PlainTextResponse("success")


@app.delete("/products/{id}")
def delete_product(
    request: Request,
    id: int,
    authenticate: Annotated[str | None, Header(alias="Authenticate")] = None,
):
    if error := maybe_contract_error(request):
        return error
    store.delete_product(id)
    return PlainTextResponse("success")


@app.put("/products/{id}/image")
async def update_product_image(
    request: Request,
    id: int,
    image: UploadFile = File(...),
) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    await image.read()
    return JSONResponse({"message": "Success"})


@app.get("/orders")
def get_orders(request: Request) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    return JSONResponse([order.model_dump(mode="json") for order in store.list_orders()])


@app.post("/orders", status_code=201)
def create_order(
    request: Request,
    payload: OrderBase,
    idempotency_key: Annotated[str | None, Header(alias="Idempotency-Key")] = None,
    authenticate: Annotated[str | None, Header(alias="Authenticate")] = None,
) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    return JSONResponse({"id": store.create_order(payload)}, status_code=201)


@app.get("/orders/{id}")
def get_order(request: Request, id: int) -> JSONResponse:
    if error := maybe_contract_error(request):
        return error
    return JSONResponse(store.example_order(id).model_dump(mode="json"))


@app.patch("/orders/{id}")
def update_order(
    request: Request,
    id: int,
    payload: OrderUpdate,
    authenticate: Annotated[str | None, Header(alias="Authenticate")] = None,
):
    if error := maybe_contract_error(request):
        return error
    store.update_order(id, payload)
    return PlainTextResponse("success")


@app.delete("/orders/{id}")
def delete_order(
    request: Request,
    id: int,
    authenticate: Annotated[str | None, Header(alias="Authenticate")] = None,
):
    if error := maybe_contract_error(request):
        return error
    store.delete_order(id)
    return PlainTextResponse("success")
