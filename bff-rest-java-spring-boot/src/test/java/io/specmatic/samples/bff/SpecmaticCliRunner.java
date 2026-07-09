package io.specmatic.samples.bff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class SpecmaticCliRunner {
  public static void main(String[] args) throws Exception {
    System.setProperty("SUT_PORT", System.getenv().getOrDefault("SUT_PORT", "8080"));
    System.setProperty("STUB_BASE_URL", System.getenv().getOrDefault("STUB_BASE_URL", "http://localhost:8090"));
    System.setProperty("KAFKA_BROKER", System.getenv().getOrDefault("KAFKA_BROKER", "localhost:9092"));

    try (ConfigurableApplicationContext context = SpringApplication.run(BffApplication.class)) {
      int exitCode = runSpecmatic();
      if (exitCode != 0) {
        throw new IllegalStateException("Specmatic Enterprise CLI failed with exit code " + exitCode);
      }
    }
  }

  private static int runSpecmatic() throws Exception {
    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    List<String> command = new ArrayList<>();
    command.add(javaBin);
    command.add("-jar");
    command.add("target/specmatic/specmatic-enterprise.jar");
    command.add("run-suite");
    command.add("--config");
    command.add("specmatic.yaml");

    Process process = new ProcessBuilder(command)
        .inheritIO()
        .directory(new File("."))
        .start();
    return process.waitFor();
  }
}
