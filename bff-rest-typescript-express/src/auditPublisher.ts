import { Kafka } from "kafkajs";
import { config } from "./config.js";

export async function publishProductQueryAudit(product: Record<string, unknown>): Promise<void> {
  if (!config.kafkaEnabled) return;

  const kafka = new Kafka({
    clientId: "bff-rest-typescript-express",
    brokers: [config.kafkaBrokerUrl]
  });
  const producer = kafka.producer();

  try {
    await producer.connect();
    await producer.send({
      topic: config.kafkaTopic,
      messages: [
        {
          value: JSON.stringify({
            name: product.name,
            inventory: product.inventory,
            id: product.id,
            categories: [{ id: 1, name: String(product.type ?? "general") }]
          })
        }
      ]
    });
  } finally {
    await producer.disconnect().catch(() => undefined);
  }
}
