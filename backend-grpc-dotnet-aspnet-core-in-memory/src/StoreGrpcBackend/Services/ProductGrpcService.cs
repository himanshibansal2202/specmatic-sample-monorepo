using Com.Store;
using Grpc.Core;

namespace StoreGrpcBackend.Services;

public sealed class ProductGrpcService(StoreRepository repository) : ProductService.ProductServiceBase
{
    public override Task<ProductListResponse> SearchProducts(ProductSearchRequest request, ServerCallContext context)
    {
        var response = new ProductListResponse();
        response.Products.AddRange(repository.SearchProducts(request.Type));
        return Task.FromResult(response);
    }

    public override Task<Product> GetProduct(ProductId request, ServerCallContext context)
    {
        if (request.Id <= 0)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Product id is required"));

        return Task.FromResult(repository.GetProduct(request.Id) ?? new Product
        {
            Id = request.Id,
            Name = "Generated Product",
            Type = ProductType.Other,
            Inventory = 10
        });
    }

    public override Task<ProductId> AddProduct(NewProduct request, ServerCallContext context)
    {
        if (string.IsNullOrWhiteSpace(request.Name) || request.Type == ProductType.NullProdType)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Product name and type are required"));

        return Task.FromResult(new ProductId { Id = repository.AddProduct(request) });
    }

    public override Task<ProductResponse> UpdateProduct(Product request, ServerCallContext context)
    {
        if (request.Id <= 0 || string.IsNullOrWhiteSpace(request.Name) || request.Type == ProductType.NullProdType || request.Inventory <= 0)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Product id, name, type and inventory are required"));

        repository.UpdateProduct(request);
        return Task.FromResult(new ProductResponse { Message = "Product updated" });
    }

    public override Task<ProductResponse> DeleteProduct(ProductId request, ServerCallContext context)
    {
        if (request.Id <= 0)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Product id is required"));

        repository.DeleteProduct(request.Id);
        return Task.FromResult(new ProductResponse { Message = "Product deleted" });
    }
}
