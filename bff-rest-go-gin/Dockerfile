FROM golang:1.26.5 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /out/bff-rest-go-gin ./cmd/server

FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /app
COPY --from=build /out/bff-rest-go-gin /app/bff-rest-go-gin
EXPOSE 8080
ENTRYPOINT ["/app/bff-rest-go-gin"]
