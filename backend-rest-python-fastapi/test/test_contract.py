import subprocess
import sys


def test_specmatic_contract_suite() -> None:
    completed = subprocess.run([sys.executable, "scripts/run_contract_tests.py"], text=True)
    assert completed.returncode == 0
