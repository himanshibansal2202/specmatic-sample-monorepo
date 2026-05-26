FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY backend-grpc-dotnet-aspnet-core-in-memory.sln ./
COPY src/StoreGrpcBackend/StoreGrpcBackend.csproj src/StoreGrpcBackend/
RUN dotnet restore src/StoreGrpcBackend/StoreGrpcBackend.csproj

COPY src/StoreGrpcBackend src/StoreGrpcBackend
RUN dotnet publish src/StoreGrpcBackend/StoreGrpcBackend.csproj -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
ENV SUT_PORT=8080
EXPOSE 8080
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "StoreGrpcBackend.dll"]
