import { createApp } from "./app.js";
import { getConfig } from "./config.js";

const config = getConfig();
const app = createApp(config);

const server = app.listen(config.port, config.host, () => {
  console.log(`BFF listening on ${config.baseUrl}`);
});

server.on("error", (error) => {
  console.error(error);
  process.exit(1);
});
