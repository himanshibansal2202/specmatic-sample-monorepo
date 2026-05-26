import { createApp } from "./app.js";
import { loadConfig } from "./config.js";

const config = loadConfig();
const app = createApp();

const server = app.listen(config.port, config.host, () => {
  console.log(`BFF listening on http://${config.host}:${config.port}`);
});

server.on("error", (error) => {
  console.error("Failed to start BFF", error);
  process.exitCode = 1;
});
