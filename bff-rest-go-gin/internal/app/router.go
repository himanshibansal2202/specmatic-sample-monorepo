package app

import (
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

type Backend interface {
	CreateProduct(c *gin.Context, body ProductBase, responseCode string) (int, []byte, string, error)
	FindProducts(c *gin.Context) (int, []byte, string, error)
	CreateOrder(c *gin.Context, body OrderBase, responseCode string) (int, []byte, string, error)
	GetOrders(c *gin.Context) (int, []byte, string, error)
}

type Publisher interface {
	PublishProduct(Product) error
	Close() error
}

type noopPublisher struct{}

func (noopPublisher) PublishProduct(Product) error { return nil }
func (noopPublisher) Close() error                 { return nil }

type Server struct {
	backend   Backend
	publisher Publisher
	mu        sync.RWMutex
	monitors  map[string]MonitorResponse
}

func NewRouter(backend Backend, publisher Publisher) *gin.Engine {
	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(gin.Recovery())

	if publisher == nil {
		publisher = noopPublisher{}
	}

	server := &Server{backend: backend, publisher: publisher, monitors: map[string]MonitorResponse{}}
	router.POST("/products", server.createProduct)
	router.GET("/findAvailableProducts", server.findAvailableProducts)
	router.POST("/orders", server.createOrder)
	router.GET("/orders", server.getOrders)
	router.GET("/monitor/:id", server.retrieveMonitor)
	return router
}

func (s *Server) createProduct(c *gin.Context) {
	var body ProductBase
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: "Invalid product request"})
		return
	}

	responseCode := c.GetHeader("Specmatic-Response-Code")
	if responseCode == "202" {
		s.setMonitor("123", MonitorResponse{
			Request: MonitorRequest{
				Method:  http.MethodPost,
				Body:    body,
				Headers: []HeaderItem{{Name: "Content-Type", Value: "application/json"}},
			},
			Response: MonitorResponseDetail{
				StatusCode: http.StatusCreated,
				Body:       IDResponse{ID: 123},
				Headers:    []HeaderItem{{Name: "Content-Type", Value: "application/json"}},
			},
		})
		c.Header("Link", "</monitor/123>;rel=related;title=monitor")
		c.Status(http.StatusAccepted)
		return
	}

	status, responseBody, contentType, err := s.backend.CreateProduct(c, body, responseCode)
	if err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: err.Error()})
		return
	}

	if status == http.StatusCreated {
		var id IDResponse
		if decoded, ok := decodeJSON[IDResponse](responseBody); ok {
			id = decoded
		}
		_ = s.publisher.PublishProduct(Product{
			ID:        id.ID,
			Name:      body.Name,
			Type:      body.Type,
			Inventory: body.Inventory,
		})
	}

	writeDependencyResponse(c, status, contentType, responseBody)
}

func (s *Server) findAvailableProducts(c *gin.Context) {
	if c.GetHeader("pageSize") == "" || c.Query("from-date") == "" || c.Query("to-date") == "" {
		c.JSON(http.StatusBadRequest, BadRequest{Message: "Missing required search parameters"})
		return
	}
	if !validOptionalProductType(c.Query("type")) || !validInteger(c.GetHeader("pageSize")) || !validDate(c.Query("from-date")) || !validDate(c.Query("to-date")) {
		c.JSON(http.StatusBadRequest, BadRequest{Message: "Invalid product search parameters"})
		return
	}
	if c.GetHeader("Specmatic-Response-Code") == "429" {
		c.Header("Retry-After", "30")
		c.Status(http.StatusTooManyRequests)
		return
	}

	status, responseBody, contentType, err := s.backend.FindProducts(c)
	if err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: err.Error()})
		return
	}
	writeDependencyResponse(c, status, contentType, responseBody)
}

func (s *Server) createOrder(c *gin.Context) {
	var body OrderBase
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: "Invalid order request"})
		return
	}

	responseCode := c.GetHeader("Specmatic-Response-Code")
	if responseCode == "202" {
		s.setMonitor("123", MonitorResponse{
			Request: MonitorRequest{
				Method:  http.MethodPost,
				Body:    body,
				Headers: []HeaderItem{{Name: "Content-Type", Value: "application/json"}},
			},
			Response: MonitorResponseDetail{
				StatusCode: http.StatusCreated,
				Body:       IDResponse{ID: 123},
				Headers:    []HeaderItem{{Name: "Content-Type", Value: "application/json"}},
			},
		})
		c.Header("Link", "</monitor/123>;rel=related;title=monitor")
		c.Status(http.StatusAccepted)
		return
	}

	status, responseBody, contentType, err := s.backend.CreateOrder(c, body, responseCode)
	if err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: err.Error()})
		return
	}
	writeDependencyResponse(c, status, contentType, responseBody)
}

func (s *Server) getOrders(c *gin.Context) {
	status, responseBody, contentType, err := s.backend.GetOrders(c)
	if err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: err.Error()})
		return
	}
	if status == http.StatusOK {
		if orders, ok := decodeJSON[[]Order](responseBody); ok {
			for i := range orders {
				if orders[i].Status == "fulfilled" {
					orders[i].Status = "completed"
				}
			}
			c.JSON(status, orders)
			return
		}
	}
	writeDependencyResponse(c, status, contentType, responseBody)
}

func (s *Server) retrieveMonitor(c *gin.Context) {
	id := c.Param("id")
	if _, err := strconv.Atoi(id); err != nil {
		c.JSON(http.StatusBadRequest, BadRequest{Message: "Invalid monitor id"})
		return
	}
	s.mu.RLock()
	monitor, ok := s.monitors[id]
	s.mu.RUnlock()
	if ok {
		c.JSON(http.StatusOK, monitor)
		return
	}
	c.JSON(http.StatusOK, defaultMonitor())
}

func defaultMonitor() MonitorResponse {
	return MonitorResponse{
		Request: MonitorRequest{
			Method: "POST",
			Body:   map[string]any{"state": "accepted"},
			Headers: []HeaderItem{
				{Name: "Content-Type", Value: "application/json"},
			},
		},
		Response: MonitorResponseDetail{
			StatusCode: http.StatusAccepted,
			Body:       map[string]any{},
			Headers: []HeaderItem{
				{Name: "Link", Value: "</monitor/123>;rel=related;title=monitor"},
			},
		},
	}
}

func (s *Server) setMonitor(id string, monitor MonitorResponse) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.monitors[id] = monitor
}

func writeDependencyResponse(c *gin.Context, status int, contentType string, body []byte) {
	if status == http.StatusAccepted {
		c.Header("Link", "</monitor/123>;rel=related;title=monitor")
		c.Status(status)
		return
	}
	if contentType == "" {
		contentType = "application/json"
	}
	c.Data(status, contentType, body)
}

func validOptionalProductType(value string) bool {
	if value == "" {
		return true
	}
	switch value {
	case "book", "food", "gadget", "other":
		return true
	default:
		return false
	}
}

func validInteger(value string) bool {
	_, err := strconv.Atoi(value)
	return err == nil
}

func validDate(value string) bool {
	_, err := time.Parse("2006-01-02", value)
	return err == nil
}
