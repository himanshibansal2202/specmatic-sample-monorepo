package main

import (
	"log"

	"github.com/specmatic-samples/bff-rest-go-gin/internal/app"
	"github.com/specmatic-samples/bff-rest-go-gin/internal/config"
	"github.com/specmatic-samples/bff-rest-go-gin/internal/dependencies"
)

func main() {
	cfg := config.Load()
	backend := dependencies.NewBackendClient(cfg.StubBaseURL, cfg.BackendAPIKey)
	publisher := dependencies.NewKafkaPublisher(cfg.KafkaBrokerURL, cfg.KafkaTopic)
	defer publisher.Close()

	router := app.NewRouter(backend, publisher)
	if err := router.Run(":" + cfg.SUTPort); err != nil {
		log.Fatal(err)
	}
}
