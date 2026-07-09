import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.FRONTEND_PORT ?? 3000),
    host: "0.0.0.0"
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./test/setup.ts"],
    testTimeout: 180000
  }
});
