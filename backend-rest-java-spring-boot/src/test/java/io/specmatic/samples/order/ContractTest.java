package io.specmatic.samples.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractTest {
    @Test
    void contract() throws Exception {
        String port = System.getenv().getOrDefault("SUT_PORT", "8080");
        System.setProperty("server.port", port);
        ConfigurableApplicationContext app = SpringApplication.run(OrderApiApplication.class);
        try {
            waitUntilHealthy("http://localhost:" + port + "/actuator/health");
            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            command.add("-jar");
            command.add("target/specmatic/specmatic-enterprise.jar");
            command.add("test");
            Process process = new ProcessBuilder(command).inheritIO().start();
            assertEquals(0, process.waitFor(), "Specmatic Enterprise contract tests failed");
        } finally {
            app.close();
        }
    }

    private void waitUntilHealthy(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        for (int attempt = 0; attempt < 40; attempt++) {
            try {
                HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) return;
            } catch (Exception ignored) {
                Thread.sleep(250);
            }
        }
        throw new IllegalStateException("Application did not become healthy at " + url);
    }
}
