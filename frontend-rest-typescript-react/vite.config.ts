import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.FRONTEND_PORT ?? 3000)
  },
  preview: {
    port: Number(process.env.FRONTEND_PORT ?? 3000)
  },
  test: {
    environment: "jsdom",
    testTimeout: 180000
  }
});
