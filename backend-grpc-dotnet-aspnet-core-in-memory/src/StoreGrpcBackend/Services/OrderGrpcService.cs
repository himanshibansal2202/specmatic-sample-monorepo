using Com.Store;
using Google.Protobuf.WellKnownTypes;
using Grpc.Core;

namespace StoreGrpcBackend.Services;

public sealed class OrderGrpcService(StoreRepository repository) : OrderService.OrderServiceBase
{
    public override Task<OrderListResponse> SearchOrders(OrderSearchRequest request, ServerCallContext context)
    {
        var response = new OrderListResponse();
        response.Orders.AddRange(repository.SearchOrders(request.ProductId, request.Status));
        return Task.FromResult(response);
    }

    public override Task<Order> GetOrder(OrderId request, ServerCallContext context)
    {
        if (request.Id <= 0)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Order id is required"));

        return Task.FromResult(repository.GetOrder(request.Id) ?? new Order
        {
            Id = request.Id,
            ProductId = 10,
            Count = 1,
            Status = OrderStatus.Pending
        });
    }

    public override Task<OrderId> AddOrder(NewOrder request, ServerCallContext context)
    {
        if (request.ProductId <= 0 || request.Count <= 0 || request.Status == OrderStatus.NullOrdStatus)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Order product id, count and status are required"));

        return Task.FromResult(new OrderId { Id = repository.AddOrder(request) });
    }

    public override Task<OrderResponse> UpdateOrder(Order request, ServerCallContext context)
    {
        if (request.Id <= 0 || request.ProductId <= 0 || request.Count <= 0 || request.Status == OrderStatus.NullOrdStatus)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Order id, product id, count and status are required"));

        repository.UpdateOrder(request);
        return Task.FromResult(new OrderResponse { Message = "Order updated" });
    }

    public override Task<OrderResponse> DeleteOrder(OrderId request, ServerCallContext context)
    {
        if (request.Id <= 0)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Order id is required"));

        repository.DeleteOrder(request.Id);
        return Task.FromResult(new OrderResponse { Message = "Order deleted" });
    }

    public override Task<Empty> EmptyOrder(Empty request, ServerCallContext context)
    {
        return Task.FromResult(new Empty());
    }
}
