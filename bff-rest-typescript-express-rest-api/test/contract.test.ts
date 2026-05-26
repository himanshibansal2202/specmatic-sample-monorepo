import { spawn } from "node:child_process";
import { mkdirSync } from "node:fs";
import { resolve } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { createApp } from "../src/app.js";
import { loadConfig } from "../src/config.js";

const specmaticImage = process.env.SPECMATIC_DOCKER_IMAGE ?? "specmatic/specmatic:latest";
const reportDir = resolve("build/reports/specmatic");
const sutBaseUrl = process.env.SUT_BASE_URL ?? "http://host.docker.internal:8080";
const stubBaseUrl = process.env.STUB_BASE_URL ?? "http://localhost:8090";
const specmaticStubBaseUrl = process.env.SPECMATIC_STUB_BASE_URL ?? "http://host.docker.internal:8090";
let mockContainerId: string | undefined;

mkdirSync(reportDir, { recursive: true });

const config = loadConfig();
const app = createApp();

try {
  mockContainerId = await startBackendMock();
  await waitForApp(stubBaseUrl);
  const server = await new Promise<ReturnType<typeof app.listen>>((resolveServer, reject) => {
    const started = app.listen(config.port, config.host, () => resolveServer(started));
    started.on("error", reject);
  });

  await waitForApp(`http://localhost:${config.port}/monitor/1`);
  try {
    await runSpecmatic();
  } finally {
    await new Promise<void>((resolveClose) => server.close(() => resolveClose()));
  }
} finally {
  if (mockContainerId) {
    await run("docker", ["stop", mockContainerId], { collectOutput: false });
  }
}

async function waitForApp(url: string): Promise<void> {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    try {
      const response = await fetch(url);
      if (response.status < 500) {
        return;
      }
    } catch {
      await delay(500);
    }
  }
  throw new Error(`Application did not become ready at ${url}`);
}

async function startBackendMock(): Promise<string> {
  const containerId = await run("docker", [
    "run",
    "-d",
    "--rm",
    "-p",
    "8090:8090",
    "-v",
    `${process.cwd()}:/usr/src/app`,
    "-w",
    "/usr/src/app",
    "-e",
    "STUB_BASE_URL=http://0.0.0.0:8090",
    specmaticImage,
    "mock",
    "--config",
    "specmatic.yaml"
  ], { collectOutput: true });

  return containerId.trim();
}

async function runSpecmatic(): Promise<void> {
  const args = [
    "run",
    "--rm",
    "-v",
    `${process.cwd()}:/usr/src/app`,
    "-w",
    "/usr/src/app",
    "-e",
    `SUT_BASE_URL=${sutBaseUrl}`,
    "-e",
    `STUB_BASE_URL=${specmaticStubBaseUrl}`,
    specmaticImage,
    "test",
    "--config",
    "specmatic.yaml"
  ];

  await run("docker", args);
}

function run(
  command: string,
  args: string[],
  options: { collectOutput?: boolean } = {}
): Promise<string> {
  return new Promise((resolveRun, reject) => {
    let output = "";
    const child = spawn(command, args, {
      stdio: options.collectOutput ? ["ignore", "pipe", "inherit"] : "inherit",
      env: {
        ...process.env,
        SUT_BASE_URL: sutBaseUrl,
        STUB_BASE_URL: stubBaseUrl
      }
    });

    child.stdout?.on("data", (chunk: Buffer) => {
      output += chunk.toString();
    });

    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) {
        resolveRun(output);
      } else {
        reject(new Error(`${command} ${args.join(" ")} exited with code ${code}`));
      }
    });
  });
}
