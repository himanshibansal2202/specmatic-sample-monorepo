package app

type ProductBase struct {
	Name      string `json:"name" binding:"required"`
	Type      string `json:"type" binding:"required,oneof=book food gadget other"`
	Inventory int    `json:"inventory" binding:"required,min=1,max=101"`
}

type Product struct {
	ID        int    `json:"id"`
	Name      string `json:"name"`
	Type      string `json:"type"`
	Inventory int    `json:"inventory"`
	CreatedOn string `json:"createdOn"`
}

type OrderBase struct {
	ProductID int `json:"productid" binding:"required"`
	Count     int `json:"count" binding:"required"`
}

type Order struct {
	ID        int    `json:"id"`
	ProductID int    `json:"productid"`
	Count     int    `json:"count"`
	Status    string `json:"status"`
}

type IDResponse struct {
	ID int `json:"id"`
}

type BadRequest struct {
	Timestamp string `json:"timestamp,omitempty"`
	Status    int    `json:"status,omitempty"`
	Error     string `json:"error,omitempty"`
	Message   string `json:"message"`
}

type MonitorResponse struct {
	Request  MonitorRequest        `json:"request"`
	Response MonitorResponseDetail `json:"response"`
}

type MonitorRequest struct {
	Method  string       `json:"method"`
	Body    any          `json:"body"`
	Headers []HeaderItem `json:"headers"`
}

type MonitorResponseDetail struct {
	StatusCode int          `json:"statusCode"`
	Body       any          `json:"body"`
	Headers    []HeaderItem `json:"headers"`
}

type HeaderItem struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

type KafkaProductMessage struct {
	Name       string          `json:"name"`
	Inventory  int             `json:"inventory"`
	ID         int             `json:"id"`
	Categories []KafkaCategory `json:"categories,omitempty"`
}

type KafkaCategory struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
}
