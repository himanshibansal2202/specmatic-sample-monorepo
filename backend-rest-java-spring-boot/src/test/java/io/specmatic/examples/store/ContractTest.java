package io.specmatic.examples.store;

import io.specmatic.test.SpecmaticJUnitSupport;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class ContractTest {
    private static ConfigurableApplicationContext application;
    private static SpecmaticJUnitSupport specmatic;

    @BeforeAll
    static void startApplication() throws IOException {
        int port = Integer.parseInt(System.getProperty("SUT_PORT", String.valueOf(availablePort())));
        String baseUrl = "http://localhost:" + port;

        System.setProperty("server.port", String.valueOf(port));
        System.setProperty("TEST_BASE_URL", baseUrl);
        System.setProperty("SUT_BASE_URL", baseUrl);
        System.setProperty("CONFIG_FILE", "specmatic.yaml");
        System.setProperty("SPECMATIC_CONFIG_FILE", "specmatic.yaml");

        application = SpringApplication.run(OrderApiApplication.class);
        specmatic = new SpecmaticJUnitSupport();
    }

    @TestFactory
    Stream<DynamicTest> contractTests() {
        return specmatic.contractTest();
    }

    @AfterAll
    static void stopApplication() {
        if (specmatic != null) {
            specmatic.report();
        }
        if (application != null) {
            application.close();
        }
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
