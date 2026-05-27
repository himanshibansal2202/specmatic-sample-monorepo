const DEFAULT_PORT = 8080;

export const config = {
  host: process.env.SUT_HOST || "127.0.0.1",
  port: Number.parseInt(process.env.SUT_PORT || `${DEFAULT_PORT}`, 10),
  baseUrl:
    process.env.SUT_BASE_URL ||
    `http://localhost:${process.env.SUT_PORT || DEFAULT_PORT}`
};
