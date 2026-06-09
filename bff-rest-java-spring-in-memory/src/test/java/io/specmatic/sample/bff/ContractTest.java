package io.specmatic.sample.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ContractTest {
    @Test
    void contractTestsPass() throws Exception {
        int sutPort = Integer.parseInt(System.getenv().getOrDefault("SUT_PORT", "8080"));
        int stubPort = Integer.parseInt(System.getenv().getOrDefault("STUB_PORT", "8090"));
        int brokerPort = Integer.parseInt(System.getenv().getOrDefault("BROKER_PORT", "9092"));
        String dockerImage = System.getenv().getOrDefault("SPECMATIC_DOCKER_IMAGE", "specmatic/enterprise:latest");

        List<String> command = new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "-v", new File(".").getAbsolutePath() + ":/usr/src/app",
                "-w", "/usr/src/app",
                "-p", stubPort + ":" + stubPort,
                "-p", brokerPort + ":" + brokerPort,
                "-e", "SPECMATIC_LICENSE_KEY",
                "-e", "SUT_BASE_URL=http://host.docker.internal:" + sutPort,
                "-e", "STUB_BASE_URL=http://0.0.0.0:" + stubPort,
                "-e", "BROKER_HOST=0.0.0.0",
                "-e", "BROKER_PORT=" + brokerPort,
                "-e", "BROKER_URL=0.0.0.0:" + brokerPort,
                dockerImage,
                "test", "--config", "specmatic.yaml"
        ));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .inheritIO()
                .start();

        assertEquals(0, process.waitFor(), "Specmatic contract tests failed");
    }
}
