from __future__ import annotations

import os

import uvicorn

if __name__ == "__main__":
    port = int(os.environ.get("SUT_PORT", "8080"))
    host = os.environ.get("SUT_HOST", "0.0.0.0")
    uvicorn.run("src.app.main:app", host=host, port=port)
