const { readFile } = require("node:fs/promises");

test("ContractTest", async () => {
  const report = JSON.parse(await readFile("build/reports/specmatic/test/ctrf/ctrf-report.json", "utf8"));
  const tests = report.results?.tests || [];
  const nonWipFailures = tests.filter((contractTest) =>
    contractTest.status === "failed" && !(contractTest.tags || []).includes("wip")
  );

  expect(tests.length).toBeGreaterThan(0);
  expect(nonWipFailures).toEqual([]);
});
