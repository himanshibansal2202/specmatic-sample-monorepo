using Com.Store;

namespace StoreGrpcBackend.Services;

public sealed class StoreRepository
{
    private readonly object sync = new();
    private readonly Dictionary<int, Product> products = new();
    private readonly Dictionary<int, Order> orders = new();
    private int nextProductId = 15;
    private int nextOrderId = 21;

    public StoreRepository()
    {
        products[1] = new Product
        {
            Id = 1,
            Name = "Effective Java",
            Type = ProductType.Book,
            Inventory = 5
        };
        products[10] = new Product
        {
            Id = 10,
            Name = "Keyboard",
            Type = ProductType.Gadget,
            Inventory = 100
        };
        orders[1] = new Order
        {
            Id = 1,
            ProductId = 10,
            Count = 2,
            Status = OrderStatus.Pending
        };
    }

    public IReadOnlyCollection<Product> SearchProducts(ProductType type)
    {
        lock (sync)
        {
            return products.Values
                .Where(product => type == ProductType.NullProdType || product.Type == type)
                .Select(Clone)
                .ToArray();
        }
    }

    public Product? GetProduct(int id)
    {
        lock (sync)
        {
            return products.TryGetValue(id, out var product) ? Clone(product) : null;
        }
    }

    public int AddProduct(NewProduct product)
    {
        lock (sync)
        {
            var id = nextProductId++;
            products[id] = new Product
            {
                Id = id,
                Name = product.Name,
                Type = product.Type,
                Inventory = product.Inventory
            };
            return id;
        }
    }

    public bool UpdateProduct(Product product)
    {
        lock (sync)
        {
            if (!products.ContainsKey(product.Id))
                return false;

            products[product.Id] = Clone(product);
            return true;
        }
    }

    public bool DeleteProduct(int id)
    {
        lock (sync)
        {
            return products.Remove(id);
        }
    }

    public IReadOnlyCollection<Order> SearchOrders(int productId, OrderStatus status)
    {
        lock (sync)
        {
            return orders.Values
                .Where(order => productId == 0 || order.ProductId == productId)
                .Where(order => status == OrderStatus.NullOrdStatus || order.Status == status)
                .Select(Clone)
                .ToArray();
        }
    }

    public Order? GetOrder(int id)
    {
        lock (sync)
        {
            return orders.TryGetValue(id, out var order) ? Clone(order) : null;
        }
    }

    public int AddOrder(NewOrder order)
    {
        lock (sync)
        {
            var id = nextOrderId++;
            orders[id] = new Order
            {
                Id = id,
                ProductId = order.ProductId,
                Count = order.Count,
                Status = order.Status
            };
            if (products.TryGetValue(order.ProductId, out var product))
                product.Inventory = Math.Max(0, product.Inventory - order.Count);

            return id;
        }
    }

    public bool UpdateOrder(Order order)
    {
        lock (sync)
        {
            if (!orders.ContainsKey(order.Id))
                return false;

            orders[order.Id] = Clone(order);
            return true;
        }
    }

    public bool DeleteOrder(int id)
    {
        lock (sync)
        {
            return orders.Remove(id);
        }
    }

    private static Product Clone(Product product) => new()
    {
        Id = product.Id,
        Name = product.Name,
        Type = product.Type,
        Inventory = product.Inventory
    };

    private static Order Clone(Order order) => new()
    {
        Id = order.Id,
        ProductId = order.ProductId,
        Count = order.Count,
        Status = order.Status
    };
}
