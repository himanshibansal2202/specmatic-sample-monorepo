export type AppConfig = {
  host: string;
  port: number;
  backendBaseUrl: string;
  backendApiKey: string;
};

export function loadConfig(): AppConfig {
  const port = Number.parseInt(process.env.SUT_PORT ?? "8080", 10);

  return {
    host: process.env.SUT_HOST ?? "0.0.0.0",
    port,
    backendBaseUrl: trimTrailingSlash(process.env.STUB_BASE_URL ?? "http://localhost:8090"),
    backendApiKey: process.env.BACKEND_API_KEY ?? "sample-api-key"
  };
}

function trimTrailingSlash(value: string): string {
  return value.endsWith("/") ? value.slice(0, -1) : value;
}
