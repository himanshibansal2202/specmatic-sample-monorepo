package dependencies

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/specmatic-samples/bff-rest-go-gin/internal/app"
)

type BackendClient struct {
	baseURL string
	apiKey  string
	client  *http.Client
}

func NewBackendClient(baseURL, apiKey string) *BackendClient {
	return &BackendClient{
		baseURL: strings.TrimRight(baseURL, "/"),
		apiKey:  apiKey,
		client:  &http.Client{Timeout: 5 * time.Second},
	}
}

func (b *BackendClient) CreateProduct(c *gin.Context, body app.ProductBase, responseCode string) (int, []byte, string, error) {
	payload, err := json.Marshal(body)
	if err != nil {
		return 0, nil, "", err
	}
	headers := map[string]string{
		"Authenticate":    b.apiKey,
		"Idempotency-Key": idempotencyKey(c),
	}
	if responseCode != "" {
		headers["Specmatic-Response-Code"] = responseCode
	}
	return b.do(http.MethodPost, "/products", payload, headers)
}

func (b *BackendClient) FindProducts(c *gin.Context) (int, []byte, string, error) {
	path := "/products"
	values := c.Request.URL.Query()
	if encoded := values.Encode(); encoded != "" {
		path += "?" + encoded
	}
	headers := map[string]string{}
	if pageSize := c.GetHeader("pageSize"); pageSize != "" {
		headers["pageSize"] = pageSize
	}
	if responseCode := c.GetHeader("Specmatic-Response-Code"); responseCode != "" {
		headers["Specmatic-Response-Code"] = responseCode
	}
	return b.do(http.MethodGet, path, nil, headers)
}

func (b *BackendClient) CreateOrder(c *gin.Context, body app.OrderBase, responseCode string) (int, []byte, string, error) {
	payload, err := json.Marshal(body)
	if err != nil {
		return 0, nil, "", err
	}
	headers := map[string]string{
		"Authenticate":    b.apiKey,
		"Idempotency-Key": idempotencyKey(c),
	}
	if responseCode != "" {
		headers["Specmatic-Response-Code"] = responseCode
	}
	return b.do(http.MethodPost, "/orders", payload, headers)
}

func (b *BackendClient) GetOrders(c *gin.Context) (int, []byte, string, error) {
	return b.do(http.MethodGet, "/orders", nil, map[string]string{})
}

func (b *BackendClient) do(method, path string, body []byte, headers map[string]string) (int, []byte, string, error) {
	var reader io.Reader
	if body != nil {
		reader = bytes.NewReader(body)
	}
	req, err := http.NewRequest(method, b.baseURL+path, reader)
	if err != nil {
		return 0, nil, "", err
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	req.Header.Set("Accept", "application/json")
	for name, value := range headers {
		req.Header.Set(name, value)
	}

	resp, err := b.client.Do(req)
	if err != nil {
		return 0, nil, "", err
	}
	defer resp.Body.Close()
	responseBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return 0, nil, "", err
	}
	if resp.StatusCode >= 400 && len(responseBody) == 0 {
		return 0, nil, "", fmt.Errorf("backend returned %d", resp.StatusCode)
	}
	return resp.StatusCode, responseBody, resp.Header.Get("Content-Type"), nil
}

func idempotencyKey(c *gin.Context) string {
	if value := c.GetHeader("Idempotency-Key"); value != "" {
		return value
	}
	return "11111111-1111-4111-8111-111111111111"
}
