package com.sporty.bettask.integration;

import tools.jackson.databind.ObjectMapper;
import com.sporty.bettask.integration.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        KAFKA_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    @LocalServerPort
    protected int port;

    @org.springframework.beans.factory.annotation.Autowired
    protected ObjectMapper objectMapper;

    protected IntegrationTestSupport testSupport;

    @BeforeEach
    void initTestSupport() {
        testSupport = new IntegrationTestSupport(objectMapper, port);
    }
}
