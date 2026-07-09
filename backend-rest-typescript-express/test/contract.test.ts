import { createApp } from "../src/app.js";

process.env.TESTCONTAINERS_RYUK_DISABLED ??= "true";
const { GenericContainer, Wait } = await import("testcontainers");

type Level = "none" | "positiveOnly" | "all";

const projectRoot = new URL("..", import.meta.url).pathname;
const port = Number(process.env.SUT_PORT ?? "8080");
const image = "specmatic/enterprise:1.19.1";
const levels: Level[] = ["none", "positiveOnly", "all"];

function startApp() {
  const app = createApp();
  return new Promise<import("node:http").Server>((resolve, reject) => {
    const server = app.listen(port);
    server.once("listening", () => resolve(server));
    server.once("error", reject);
  });
}

function stopApp(server: import("node:http").Server) {
  return new Promise<void>((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()));
  });
}

async function runSpecmatic(level: Level) {
  let output = "";

  const script = [
    "/usr/local/bin/enterprise run-suite --config /workspace/specmatic.yaml",
    "status=$?",
    "echo $status > /tmp/specmatic-exit-code",
    "echo __SPECMATIC_DONE__",
    "sleep 300"
  ].join("; ");

  const container = await new GenericContainer(image)
    .withBindMounts([{ source: projectRoot, target: "/workspace" }])
    .withWorkingDir("/workspace")
    .withEnvironment({
      SUT_BASE_URL: `http://host.docker.internal:${port}`,
      SCHEMA_RESILIENCY_TESTS: level,
      SPECMATIC_MIN_COVERAGE: level === "all" ? "68" : "0",
      SPECMATIC_MAX_MISSED_OPERATIONS: level === "all" ? "0" : "31",
      SPECMATIC_COVERAGE_ENFORCE: level === "all" ? "true" : "false",
      SPECMATIC_LICENSE_KEY: process.env.SPECMATIC_LICENSE_KEY ?? ""
    })
    .withEntrypoint(["/bin/sh", "-lc"])
    .withCommand([script])
    .withAutoRemove(false)
    .withLogConsumer((stream) => {
      stream.on("data", (line) => {
        output += line.toString();
        process.stdout.write(line);
      });
      stream.on("err", (line) => {
        output += line.toString();
        process.stderr.write(line);
      });
    })
    .withWaitStrategy(Wait.forLogMessage("__SPECMATIC_DONE__"))
    .withStartupTimeout(300_000)
    .start();

  const exitResult = await container.exec(["cat", "/tmp/specmatic-exit-code"]);
  try {
    await container.stop();
  } catch (error) {
    console.warn(`Container cleanup warning: ${error instanceof Error ? error.message : String(error)}`);
  }
  const summary = output.match(/Tests run:\s*(\d+),\s*Successes:\s*(\d+),\s*Failures:\s*(\d+),(?:\s*WIP:\s*\d+,)?\s*Errors:\s*(\d+)/);
  const tests = summary ? Number(summary[1]) : 0;
  const failures = summary ? Number(summary[3]) + Number(summary[4]) : 0;
  const parsedStatusCode = Number(exitResult.stdout.trim());
  const statusCode = Number.isNaN(parsedStatusCode) && summary && failures === 0 ? 0 : parsedStatusCode;

  if (statusCode !== 0 || !summary || failures > 0) {
    throw new Error(`Specmatic ${level} failed with exit ${statusCode || "unknown"}`);
  }

  return { level, tests };
}

async function main() {
  const coverage = [];
  for (const level of levels) {
    console.log(`\n--- Running Specmatic schemaResiliencyTests=${level} ---`);
    const server = await startApp();
    try {
      coverage.push(await runSpecmatic(level));
    } finally {
      await stopApp(server);
    }
  }

  for (let index = 1; index < coverage.length; index += 1) {
    if (coverage[index].tests <= coverage[index - 1].tests) {
      throw new Error(`Specmatic test count did not increase from ${coverage[index - 1].level} to ${coverage[index].level}`);
    }
  }

  console.log("\nSpecmatic progressive verification passed:");
  for (const item of coverage) {
    console.log(`${item.level}: ${item.tests} tests`);
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
