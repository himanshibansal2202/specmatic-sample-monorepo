import os


def sut_port() -> int:
    return int(os.getenv("SUT_PORT", "8080"))


def sut_base_url() -> str:
    return os.getenv("SUT_BASE_URL", f"http://localhost:{sut_port()}")
