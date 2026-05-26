import { setTimeout as delay } from "node:timers/promises";
import { execa } from "execa";

export interface SpecmaticMock {
  endpoint: string;
  stop: () => Promise<void>;
}

export async function startSpecmaticMock(): Promise<SpecmaticMock> {
  const port = Number(process.env.STUB_PORT ?? 9000);
  const endpointHost = process.env.STUB_ENDPOINT_HOST ?? "localhost";
  const endpoint = `http://${endpointHost}:${port}/graphql`;
  const output: string[] = [];
  const child = execa(
    "docker",
    [
      "run",
      "--rm",
      "-p",
      `${port}:9000`,
      "-v",
      `${process.cwd()}:/usr/src/app`,
      "-v",
      "/usr/src/app/node_modules",
      "-w",
      "/usr/src/app",
      "-e",
      "STUB_HOST=0.0.0.0",
      "-e",
      `SPECMATIC_LICENSE_PATH=${process.env.SPECMATIC_LICENSE_PATH ?? ""}`,
      process.env.SPECMATIC_IMAGE ?? "specmatic/enterprise:latest",
      "mock"
    ],
    {
      env: {
        ...process.env
      },
      stdout: "pipe",
      stderr: "pipe"
    }
  );
  child.stdout?.on("data", (chunk: Buffer) => output.push(chunk.toString()));
  child.stderr?.on("data", (chunk: Buffer) => output.push(chunk.toString()));

  await waitForGraphql(endpoint, child, output);

  return {
    endpoint,
    stop: async () => {
      child.kill("SIGTERM");
      await child.catch(() => undefined);
    }
  };
}

async function waitForGraphql(
  endpoint: string,
  child: ReturnType<typeof execa>,
  output: string[]
): Promise<void> {
  const deadline = Date.now() + 120_000;
  let lastError = "";

  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`Specmatic mock exited early.\n${output.join("")}`);
    }

    if (output.join("").includes("Stub server is running")) {
      return;
    }

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query: "{ __typename }" })
      });

      if (response.status < 500) {
        return;
      }

      lastError = `HTTP ${response.status}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }

    await delay(1_000);
  }

  child.kill("SIGTERM");
  throw new Error(`Specmatic mock did not become ready: ${lastError}\n${output.join("")}`);
}
