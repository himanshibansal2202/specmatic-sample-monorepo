import os
import re
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "tools" / "specmatic-enterprise-executable-all-1.19.1.jar"
sys.path.insert(0, str(ROOT / "src"))

from order_api.config import sut_base_url, sut_port


def wait_for_app(base_url: str, process: subprocess.Popen[str]) -> None:
    deadline = time.time() + 30
    last_error = None
    while time.time() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"Application exited before tests started with code {process.returncode}")
        try:
            response = requests.get(f"{base_url}/health", timeout=1)
            if response.status_code == 200:
                return
        except requests.RequestException as exc:
            last_error = exc
        time.sleep(0.5)
    raise RuntimeError(f"Application did not become ready at {base_url}: {last_error}")


def ensure_port_free(port: int) -> None:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        if sock.connect_ex(("127.0.0.1", port)) == 0:
            raise RuntimeError(f"Port {port} is already in use. Set SUT_PORT and SUT_BASE_URL to free values.")


def parse_summary(output: str) -> tuple[int, int, int]:
    match = re.search(r"Tests run: (\d+), Successes: (\d+), Failures: (\d+)(?:, WIP: \d+)?, Errors: (\d+)", output)
    if not match:
        return (0, 0, 0)
    tests = int(match.group(1))
    failures = int(match.group(3)) + int(match.group(4))
    return (tests, tests - failures, failures)


def main() -> int:
    os.chdir(ROOT)
    if not JAR.exists():
        subprocess.run([sys.executable, "scripts/download_specmatic.py"], check=True)

    port = sut_port()
    base_url = sut_base_url()
    ensure_port_free(port)

    env = os.environ.copy()
    env["PYTHONPATH"] = str(ROOT / "src")
    env["SUT_BASE_URL"] = base_url

    app = subprocess.Popen(
        [
            sys.executable,
            "-m",
            "uvicorn",
            "order_api.app:app",
            "--host",
            "0.0.0.0",
            "--port",
            str(port),
            "--no-access-log",
        ],
        cwd=ROOT,
        env=env,
        text=True,
    )
    try:
        wait_for_app(base_url, app)
        command = ["java", "-jar", str(JAR), "run-suite", "--config", "specmatic.yaml"]
        completed = subprocess.run(command, cwd=ROOT, env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        print(completed.stdout)
        tests, passed, failed = parse_summary(completed.stdout)
        print(f"Specmatic summary: tests={tests} passed={passed} failed={failed}")
        return completed.returncode
    finally:
        app.send_signal(signal.SIGTERM)
        try:
            app.wait(timeout=10)
        except subprocess.TimeoutExpired:
            app.kill()
            app.wait(timeout=5)


if __name__ == "__main__":
    raise SystemExit(main())
