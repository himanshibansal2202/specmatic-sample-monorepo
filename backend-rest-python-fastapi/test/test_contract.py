from __future__ import annotations

import os
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC_VERSION = "1.18.0"
SPEC_COORDINATE = "io.specmatic.enterprise:executable-all:1.18.0"
SPEC_JAR = ROOT / ".specmatic-runtime" / f"executable-all-{SPEC_VERSION}.jar"
SPEC_JAR_URL = (
    "https://repo1.maven.org/maven2/io/specmatic/enterprise/executable-all/"
    f"{SPEC_VERSION}/executable-all-{SPEC_VERSION}.jar"
)


def _free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def _download_enterprise_jar() -> None:
    override = os.environ.get("SPECMATIC_ENTERPRISE_JAR")
    if override:
        override_path = Path(override)
        if not override_path.exists():
            raise AssertionError(f"SPECMATIC_ENTERPRISE_JAR does not exist: {override_path}")
        SPEC_JAR.parent.mkdir(parents=True, exist_ok=True)
        if not SPEC_JAR.exists() or SPEC_JAR.stat().st_size != override_path.stat().st_size:
            SPEC_JAR.write_bytes(override_path.read_bytes())
        return

    if SPEC_JAR.exists() and SPEC_JAR.stat().st_size > 0:
        return

    SPEC_JAR.parent.mkdir(parents=True, exist_ok=True)
    try:
        with urllib.request.urlopen(SPEC_JAR_URL, timeout=60) as response:
            SPEC_JAR.write_bytes(response.read())
    except (urllib.error.URLError, TimeoutError) as exc:
        raise AssertionError(f"Could not download {SPEC_COORDINATE} from Maven Central: {exc}") from exc


def _wait_for_health(base_url: str, process: subprocess.Popen[str]) -> None:
    deadline = time.time() + 20
    health_url = f"{base_url}/health"
    last_error = None
    while time.time() < deadline:
        if process.poll() is not None:
            raise AssertionError(f"Application exited before contract tests started with code {process.returncode}")
        try:
            with urllib.request.urlopen(health_url, timeout=1) as response:
                if response.status == 200:
                    return
        except Exception as exc:  # noqa: BLE001 - surface final startup error below
            last_error = exc
        time.sleep(0.25)
    raise AssertionError(f"Application did not become healthy at {health_url}: {last_error}")


def test_ContractTest() -> None:
    _download_enterprise_jar()

    port = int(os.environ.get("SUT_PORT", "0")) or _free_port()
    host = os.environ.get("SUT_HOST", "127.0.0.1")
    base_url = os.environ.get("SUT_BASE_URL", f"http://localhost:{port}")
    env = os.environ.copy()
    env.update(
        {
            "SUT_PORT": str(port),
            "SUT_BASE_URL": base_url,
            "SUT_ACTUATOR_URL": os.environ.get("SUT_ACTUATOR_URL", f"{base_url}/openapi.json"),
        }
    )

    reports_dir = ROOT / "build" / "reports" / "specmatic"
    reports_dir.mkdir(parents=True, exist_ok=True)
    app_process = subprocess.Popen(
        [
            sys.executable,
            "-m",
            "uvicorn",
            "src.app.main:app",
            "--host",
            host,
            "--port",
            str(port),
        ],
        cwd=ROOT,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )

    try:
        _wait_for_health(base_url, app_process)
        command = ["java", "-jar", str(SPEC_JAR), "test"]
        result = subprocess.run(command, cwd=ROOT, env=env, text=True, capture_output=True, timeout=300)
        output = result.stdout + result.stderr
        (reports_dir / "specmatic.log").write_text(output, encoding="utf-8")
        assert result.returncode == 0, output
        assert "Failures: 0" in output or "failure: 0" in output.lower() or "0 failures" in output.lower(), output
    finally:
        app_process.terminate()
        try:
            app_output, _ = app_process.communicate(timeout=10)
        except subprocess.TimeoutExpired:
            app_process.kill()
            app_output, _ = app_process.communicate(timeout=10)
        (reports_dir / "app.log").write_text(app_output or "", encoding="utf-8")
