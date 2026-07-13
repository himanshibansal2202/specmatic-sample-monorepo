import { config } from "./config.js";

type JsonValue = Record<string, unknown> | Array<unknown>;

async function readBody(response: Response): Promise<JsonValue | string | undefined> {
  const text = await response.text();
  if (!text) return undefined;
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return JSON.parse(text) as JsonValue;
  }
  return text;
}

export interface BackendResult {
  status: number;
  body?: JsonValue | string;
  contentType?: string;
}

export async function callBackend(
  method: string,
  path: string,
  options: { body?: unknown; headers?: Record<string, string> } = {}
): Promise<BackendResult> {
  const headers: Record<string, string> = {
    Accept: "application/json",
    Authenticate: "sample-api-key",
    "Idempotency-Key": "11111111-1111-4111-8111-111111111111",
    ...options.headers
  };

  let body: string | undefined;
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }

  const response = await fetch(`${config.backendBaseUrl}${path}`, { method, headers, body });
  return {
    status: response.status,
    body: await readBody(response),
    contentType: response.headers.get("content-type") ?? undefined
  };
}
