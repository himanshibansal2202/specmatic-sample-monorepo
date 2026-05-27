import { spawn } from "node:child_process";
import { once } from "node:events";
import { readFile } from "node:fs/promises";
import process from "node:process";

const port = Number.parseInt(process.env.SUT_PORT || "8080", 10);
const baseUrl = process.env.SUT_BASE_URL || `http://localhost:${port}`;

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForServer(url) {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${url}/products`);
      if (response.status < 500) return;
    } catch {
      await wait(250);
    }
  }
  throw new Error(`Server did not become ready at ${url}`);
}

async function run(command, args, options) {
  const child = spawn(command, args, {
    stdio: "inherit",
    shell: process.platform === "win32",
    ...options
  });
  const [code, signal] = await once(child, "exit");
  if (code !== 0) {
    throw new Error(`${command} ${args.join(" ")} failed with ${signal || code}`);
  }
}

async function assertSpecmaticReport() {
  const reportPath = "build/reports/specmatic/test/ctrf/ctrf-report.json";
  const report = JSON.parse(await readFile(reportPath, "utf8"));
  const summary = report.results?.summary;
  if (!summary) {
    throw new Error(`Specmatic report did not include a CTRF summary at ${reportPath}`);
  }
  const failed = summary.failed || 0;
  const errors = summary.errors || 0;
  if (failed !== 0 || errors !== 0) {
    throw new Error(
      `Specmatic reported ${failed} failures and ${errors} errors out of ${summary.tests} tests`
    );
  }
}

async function main() {
  const server = spawn(process.execPath, ["src/server.js"], {
    stdio: "inherit",
    env: {
      ...process.env,
      SUT_PORT: `${port}`,
      SUT_BASE_URL: baseUrl
    }
  });

  try {
    await waitForServer(baseUrl);
    await run("npx", ["specmatic", "test"], {
      env: {
        ...process.env,
        SUT_PORT: `${port}`,
        SUT_BASE_URL: baseUrl
      }
    });
    await assertSpecmaticReport();
  } finally {
    server.kill("SIGTERM");
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
