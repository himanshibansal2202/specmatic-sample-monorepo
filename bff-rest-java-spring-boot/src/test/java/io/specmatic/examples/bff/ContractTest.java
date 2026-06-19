package io.specmatic.examples.bff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class ContractTest {
  @Test
  void contractTestsPassThroughSpecmaticEnterpriseCli() throws Exception {
    String port = System.getenv().getOrDefault("SUT_PORT", "8080");
    String appUrl = System.getenv().getOrDefault("SUT_BASE_URL", "http://localhost:" + port);
    String stubBaseUrl = System.getenv().getOrDefault("STUB_BASE_URL", "http://localhost:8090");

    try (ConfigurableApplicationContext ignored = startApplication(port, stubBaseUrl)) {
      Process process = startSpecmatic(appUrl, stubBaseUrl);
      boolean exited = process.waitFor(5, TimeUnit.MINUTES);
      if (!exited) {
        process.destroyForcibly();
        throw new AssertionError("Specmatic Enterprise CLI did not finish within 5 minutes");
      }
      String output = Files.readString(Path.of("target", "specmatic", "specmatic-output.log"));
      assertEquals(0, process.exitValue(), "Specmatic Enterprise contract tests failed\n" + output);
    }
  }

  private ConfigurableApplicationContext startApplication(String port, String stubBaseUrl) {
    SpringApplication application = new SpringApplication(BffApplication.class);
    return application.run(
        "--server.port=" + port,
        "--backend.base-url=" + stubBaseUrl);
  }

  private Process startSpecmatic(String appUrl, String stubBaseUrl) throws IOException {
    Path jar = Path.of("target", "specmatic", "specmatic-enterprise.jar");
    if (!Files.exists(jar)) {
      throw new IOException("Missing Specmatic Enterprise CLI jar at " + jar);
    }

    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("-jar");
    command.add(jar.toString());
    command.add("run-suite");

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(Path.of("target", "specmatic", "specmatic-output.log").toFile());
    processBuilder.environment().put("APP_URL", appUrl);
    processBuilder.environment().put("SUT_BASE_URL", appUrl);
    processBuilder.environment().put("STUB_BASE_URL", stubBaseUrl);
    processBuilder.environment().put("BROKER_HOST", System.getenv().getOrDefault("BROKER_HOST", "localhost"));
    processBuilder.environment().put("BROKER_PORT", System.getenv().getOrDefault("BROKER_PORT", "9092"));
    processBuilder.environment().put("BROKER_URL", System.getenv().getOrDefault("BROKER_URL", "localhost:9092"));
    return processBuilder.start();
  }
}
