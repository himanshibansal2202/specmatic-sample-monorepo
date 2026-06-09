import { spawn } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import waitOn from "wait-on";
import { startHttpStub, stopHttpStub, test as specmaticTest } from "specmatic";

const stateFile = new URL("./contract-state.json", import.meta.url);
const ctrfReport = new URL("../build/reports/specmatic/test/ctrf/ctrf-report.json", import.meta.url);

function specmaticArgs() {
  return ["--config=specmatic.yaml"];
}

async function startApp() {
  const child = spawn(process.execPath, ["src/server.js"], {
    env: {
      ...process.env,
      SUT_PORT: process.env.SUT_PORT || "8080",
      SUT_BASE_URL: process.env.SUT_BASE_URL || "http://localhost:8080",
      STUB_BASE_URL: process.env.STUB_BASE_URL || "http://localhost:8090"
    },
    stdio: "inherit"
  });

  await waitOn({
    resources: [process.env.SUT_BASE_URL || "http://localhost:8080/health"],
    timeout: 30000
  });

  return child;
}

async function readContractSummary() {
  const report = JSON.parse(await readFile(ctrfReport, "utf8"));
  const tests = report.results?.tests || [];
  const nonWipFailures = tests.filter((contractTest) =>
    contractTest.status === "failed" && !(contractTest.tags || []).includes("wip")
  );

  return {
    total: report.results?.summary?.tests || tests.length,
    passed: report.results?.summary?.passed || 0,
    failed: report.results?.summary?.failed || 0,
    wipFailures: tests.filter((contractTest) =>
      contractTest.status === "failed" && (contractTest.tags || []).includes("wip")
    ).length,
    nonWipFailures
  };
}

export default async function globalSetup() {
  let httpStub;
  let app;

  try {
    httpStub = await startHttpStub("localhost", Number(process.env.STUB_PORT || 8090), specmaticArgs());
    app = await startApp();
    const result = await specmaticTest(undefined, undefined, undefined, specmaticArgs());
    if (!result || result.total === 0) {
      throw new Error("Specmatic did not produce any contract test results");
    }
    const summary = await readContractSummary();
    if (summary.nonWipFailures.length > 0) {
      throw new Error(`Specmatic contract tests failed: ${summary.nonWipFailures.length} non-WIP failures of ${summary.total}`);
    }

    if (app?.pid) {
      process.kill(app.pid);
      app = undefined;
    }
    if (httpStub) {
      await stopHttpStub(httpStub);
      httpStub = undefined;
    }

    await writeFile(stateFile, JSON.stringify({
      result,
      summary
    }, null, 2));
  } catch (error) {
    if (app?.pid) {
      try {
        process.kill(app.pid);
      } catch (_killError) {
        // The app may already have exited after a failed contract run.
      }
    }
    if (httpStub) {
      await stopHttpStub(httpStub);
    }
    throw error;
  }
}

export { stateFile };
