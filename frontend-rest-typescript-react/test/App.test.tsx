import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { startSpecmaticBffMock } from "./specmaticMock";

let mock: Awaited<ReturnType<typeof startSpecmaticBffMock>>;
let App: typeof import("../src/App").default;

beforeAll(async () => {
  mock = await startSpecmaticBffMock();
  import.meta.env.VITE_BFF_BASE_URL = mock.baseUrl;
  App = (await import("../src/App")).default;
});

afterAll(async () => {
  await mock?.stop();
});

describe("React storefront", () => {
  it("runs the product search workflow against the Specmatic mock", async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole("button", { name: "Search" }));

    expect(await screen.findByText(/Loaded \d+ available product/)).toBeInTheDocument();
    expect(await screen.findByText("iPhone")).toBeInTheDocument();
  });
});
