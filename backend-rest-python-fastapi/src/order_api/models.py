from datetime import date
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class ProductType(str, Enum):
    book = "book"
    food = "food"
    gadget = "gadget"
    other = "other"


class OrderStatus(str, Enum):
    fulfilled = "fulfilled"
    pending = "pending"
    cancelled = "cancelled"


class ProductBase(BaseModel):
    model_config = ConfigDict(extra="ignore")

    name: str
    type: ProductType
    inventory: int = Field(ge=1, le=101)


class Product(ProductBase):
    id: int
    createdOn: date


class OrderBase(BaseModel):
    model_config = ConfigDict(extra="ignore")

    productid: int
    count: int


class Order(OrderBase):
    id: int
    status: OrderStatus


class OrderUpdate(OrderBase):
    status: OrderStatus
