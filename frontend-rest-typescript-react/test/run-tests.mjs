import { spawnSync } from "node:child_process";

const env = { ...process.env };

if (!env.DOCKER_HOST) {
  const context = spawnSync("docker", ["context", "inspect", "--format", "{{.Endpoints.docker.Host}}"], {
    encoding: "utf8"
  });
  const dockerHost = context.stdout.trim();
  if (context.status === 0 && dockerHost) {
    env.DOCKER_HOST = dockerHost;
  }
}

if (env.DOCKER_HOST?.includes(".colima/default/docker.sock")) {
  env.TESTCONTAINERS_RYUK_DISABLED ??= "true";
}

const result = spawnSync(
  "npx",
  ["vitest", "run", "--pool=forks", "--no-file-parallelism", "--maxWorkers=1"],
  {
    stdio: "inherit",
    env
  }
);

process.exit(result.status ?? 1);
