export type AppConfig = {
  port: number;
  baseUrl: string;
};

export function loadConfig(): AppConfig {
  const port = Number(process.env.SUT_PORT ?? "8080");
  return {
    port,
    baseUrl: process.env.SUT_BASE_URL ?? `http://localhost:${port}`
  };
}
