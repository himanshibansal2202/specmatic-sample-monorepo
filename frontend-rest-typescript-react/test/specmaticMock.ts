import { GenericContainer, StartedTestContainer, Wait } from "testcontainers";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SPEC_PATH = "io/specmatic/examples/store/openapi/product_search_bff_v6.yaml";
const __dirname = path.dirname(fileURLToPath(import.meta.url));

export async function startSpecmaticBffMock(): Promise<{ baseUrl: string; stop: () => Promise<void> }> {
  const projectRoot = path.resolve(__dirname, "..");
  const container: StartedTestContainer = await new GenericContainer("specmatic/enterprise:1.19.1")
    .withBindMounts([{ source: projectRoot, target: "/workspace", mode: "rw" }])
    .withWorkingDir("/workspace")
    .withEnvironment({
      SPECMATIC_LICENSE_KEY: process.env.SPECMATIC_LICENSE_KEY ?? "",
      STUB_BASE_URL: "http://0.0.0.0:8090"
    })
    .withCommand(["stub", "--config", "specmatic.yaml"])
    .withExposedPorts(8090)
    .withWaitStrategy(Wait.forLogMessage(/Mock server is running/i))
    .start();

  return {
    baseUrl: `http://${container.getHost()}:${container.getMappedPort(8090)}`,
    stop: async () => {
      await container.stop();
    }
  };
}

export { SPEC_PATH };
