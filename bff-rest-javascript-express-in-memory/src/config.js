export function getConfig(env = process.env) {
  const port = Number(env.SUT_PORT || env.PORT || 8080);
  const stubBaseUrl = env.STUB_BASE_URL || "http://localhost:8090";

  return {
    port,
    host: env.SUT_HOST || "0.0.0.0",
    baseUrl: env.SUT_BASE_URL || `http://localhost:${port}`,
    stubBaseUrl,
    backendAuth: env.BACKEND_AUTHENTICATE || "sample-api-key",
    idempotencyKey: env.IDEMPOTENCY_KEY || "00000000-0000-4000-8000-000000000001"
  };
}
