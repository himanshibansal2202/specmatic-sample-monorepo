package config

import "os"

type Config struct {
	SUTPort       string
	StubBaseURL   string
	BackendAPIKey string
	KafkaBrokerURL string
	KafkaTopic     string
}

func Load() Config {
	return Config{
		SUTPort:       getenv("SUT_PORT", "8080"),
		StubBaseURL:   getenv("STUB_BASE_URL", "http://localhost:8090"),
		BackendAPIKey: getenv("BACKEND_API_KEY", "sample-api-key"),
		KafkaBrokerURL: getenv("KAFKA_BROKER_URL", "localhost:9092"),
		KafkaTopic:     getenv("KAFKA_TOPIC", "product-queries"),
	}
}

func getenv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
