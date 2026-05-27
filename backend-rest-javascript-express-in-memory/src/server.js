import { createApp } from "./app.js";
import { config } from "./config.js";

const app = createApp();

const server = app.listen(config.port, config.host, () => {
  console.log(`Order API listening at ${config.baseUrl}`);
});

server.on("error", (error) => {
  console.error(`Failed to start Order API on ${config.host}:${config.port}`, error);
  process.exitCode = 1;
});

process.on("SIGTERM", () => {
  server.close(() => process.exit(0));
});
