export const config = {
  port: Number(process.env.SUT_PORT ?? "8080"),
  backendBaseUrl: process.env.STUB_BASE_URL ?? "http://localhost:8090",
  kafkaBrokerUrl: process.env.KAFKA_BROKER_URL ?? "localhost:9092",
  kafkaTopic: process.env.KAFKA_TOPIC ?? "product-queries",
  kafkaEnabled: process.env.KAFKA_ENABLED !== "false"
};
