using Microsoft.AspNetCore.Server.Kestrel.Core;
using StoreGrpcBackend.Services;

var builder = WebApplication.CreateBuilder(args);

var port = int.Parse(Environment.GetEnvironmentVariable("SUT_PORT") ?? "8080");

builder.WebHost.ConfigureKestrel(options =>
{
    options.ListenAnyIP(port, listenOptions =>
    {
        listenOptions.Protocols = HttpProtocols.Http2;
    });
});

builder.Services.AddGrpc();
builder.Services.AddSingleton<StoreRepository>();

var app = builder.Build();

app.MapGrpcService<ProductGrpcService>();
app.MapGrpcService<OrderGrpcService>();
app.MapGet("/", () => "Store gRPC backend is running.");

app.Run();
