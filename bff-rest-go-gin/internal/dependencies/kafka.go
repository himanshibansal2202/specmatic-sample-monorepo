package dependencies

import (
	"encoding/json"
	"sync"

	"github.com/IBM/sarama"
	"github.com/specmatic-samples/bff-rest-go-gin/internal/app"
)

type KafkaPublisher struct {
	brokerURL string
	topic     string
	mu        sync.Mutex
	producer  sarama.SyncProducer
}

func NewKafkaPublisher(brokerURL, topic string) *KafkaPublisher {
	return &KafkaPublisher{brokerURL: brokerURL, topic: topic}
}

func (p *KafkaPublisher) PublishProduct(product app.Product) error {
	if p == nil {
		return nil
	}
	producer, err := p.getProducer()
	if err != nil {
		return err
	}
	payload := app.KafkaProductMessage{
		ID:        product.ID,
		Name:      product.Name,
		Inventory: product.Inventory,
		Categories: []app.KafkaCategory{
			{ID: 1, Name: product.Type},
		},
	}
	value, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	_, _, err = producer.SendMessage(&sarama.ProducerMessage{
		Topic: p.topic,
		Value: sarama.ByteEncoder(value),
	})
	return err
}

func (p *KafkaPublisher) getProducer() (sarama.SyncProducer, error) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if p.producer != nil {
		return p.producer, nil
	}
	cfg := sarama.NewConfig()
	cfg.Producer.Return.Successes = true
	cfg.Producer.RequiredAcks = sarama.WaitForLocal
	producer, err := sarama.NewSyncProducer([]string{p.brokerURL}, cfg)
	if err != nil {
		return nil, err
	}
	p.producer = producer
	return p.producer, nil
}

func (p *KafkaPublisher) Close() error {
	if p == nil || p.producer == nil {
		return nil
	}
	return p.producer.Close()
}
