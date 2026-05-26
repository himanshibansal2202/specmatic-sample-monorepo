using System.Diagnostics;
using DotNet.Testcontainers.Builders;
using DotNet.Testcontainers.Containers;
using Xunit;

namespace StoreGrpcBackend.ContractTests;

public sealed class SpecmaticContractTests
{
    [Fact]
    public async Task GrpcContractTestsPass()
    {
        var sampleRoot = FindSampleRoot();
        var port = GetFreePort();
        await using var app = await StartedProcess.Create(
            "dotnet",
            $"run --no-build --project {Path.Combine(sampleRoot, "src", "StoreGrpcBackend", "StoreGrpcBackend.csproj")}",
            sampleRoot,
            new Dictionary<string, string?>
            {
                ["SUT_PORT"] = port.ToString(),
                ["ASPNETCORE_ENVIRONMENT"] = "Test"
            });

        await WaitForGrpcPort(port, TimeSpan.FromSeconds(30));
        StageGrpcImports(sampleRoot);

        var specmaticImage = Environment.GetEnvironmentVariable("SPECMATIC_IMAGE") ?? "specmatic/enterprise:latest";
        var bindMountRoot = Environment.GetEnvironmentVariable("HOST_SAMPLE_ROOT") ?? sampleRoot;
        var reportDir = Path.Combine(sampleRoot, "build", "reports", "specmatic");
        Directory.CreateDirectory(reportDir);

        var container = new ContainerBuilder()
            .WithImage(specmaticImage)
            .WithBindMount(bindMountRoot, "/usr/src/app")
            .WithWorkingDirectory("/usr/src/app")
            .WithEnvironment("SPECMATIC_SUT_HOST", "host.testcontainers.internal")
            .WithEnvironment("SUT_PORT", port.ToString())
            .WithEnvironment("PROTOC_VERSION", Environment.GetEnvironmentVariable("PROTOC_VERSION") ?? "3.23.4")
            .WithExtraHost("host.testcontainers.internal", "host-gateway")
            .WithEntrypoint("/bin/sh")
            .WithCommand("-c", "git config --global --add safe.directory /usr/src/app/.specmatic/repos/specmatic-order-contracts; specmatic test --config specmatic.yaml; code=$?; echo SPECMATIC_EXIT_CODE=$code; sleep 300")
            .WithWaitStrategy(Wait.ForUnixContainer().UntilMessageIsLogged("SPECMATIC_EXIT_CODE="))
            .Build();

        await container.StartAsync();
        var stdout = await container.GetLogsAsync();

        await container.StopAsync();
        var logs = stdout.Stdout + stdout.Stderr;
        Assert.True(logs.Contains("SPECMATIC_EXIT_CODE=0"), logs);
    }

    private static void StageGrpcImports(string sampleRoot)
    {
        var source = Path.Combine(sampleRoot, "src", "StoreGrpcBackend", "Protos");
        var target = Path.Combine(sampleRoot, ".specmatic_grpc_working_dir");
        if (Directory.Exists(target))
            Directory.Delete(target, recursive: true);

        CopyDirectory(source, target);
    }

    private static void CopyDirectory(string source, string target)
    {
        Directory.CreateDirectory(target);
        foreach (var file in Directory.EnumerateFiles(source))
        {
            File.Copy(file, Path.Combine(target, Path.GetFileName(file)), overwrite: true);
        }

        foreach (var directory in Directory.EnumerateDirectories(source))
        {
            CopyDirectory(directory, Path.Combine(target, Path.GetFileName(directory)));
        }
    }

    private static string FindSampleRoot()
    {
        var current = new DirectoryInfo(AppContext.BaseDirectory);
        while (current is not null)
        {
            if (File.Exists(Path.Combine(current.FullName, "specmatic.yaml")))
                return current.FullName;
            current = current.Parent;
        }
        throw new InvalidOperationException("Could not find sample root containing specmatic.yaml.");
    }

    private static int GetFreePort()
    {
        using var listener = new System.Net.Sockets.TcpListener(System.Net.IPAddress.Loopback, 0);
        listener.Start();
        return ((System.Net.IPEndPoint)listener.LocalEndpoint).Port;
    }

    private static async Task WaitForGrpcPort(int port, TimeSpan timeout)
    {
        var deadline = DateTimeOffset.UtcNow + timeout;
        Exception? lastError = null;
        while (DateTimeOffset.UtcNow < deadline)
        {
            try
            {
                using var client = new System.Net.Sockets.TcpClient();
                await client.ConnectAsync("127.0.0.1", port);
                return;
            }
            catch (Exception ex)
            {
                lastError = ex;
                await Task.Delay(250);
            }
        }
        throw new TimeoutException($"gRPC service did not listen on port {port}.", lastError);
    }

    private sealed class StartedProcess : IAsyncDisposable
    {
        private readonly Process process;

        private StartedProcess(Process process)
        {
            this.process = process;
        }

        public static async Task<StartedProcess> Create(string fileName, string arguments, string workingDirectory, IDictionary<string, string?> environment)
        {
            var startInfo = new ProcessStartInfo(fileName, arguments)
            {
                WorkingDirectory = workingDirectory,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };

            foreach (var (key, value) in environment)
                startInfo.Environment[key] = value;

            var process = Process.Start(startInfo) ?? throw new InvalidOperationException("Failed to start application process.");
            process.OutputDataReceived += (_, e) => { if (e.Data is not null) Console.WriteLine(e.Data); };
            process.ErrorDataReceived += (_, e) => { if (e.Data is not null) Console.Error.WriteLine(e.Data); };
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            await Task.Delay(250);
            if (process.HasExited)
                throw new InvalidOperationException($"Application exited during startup with code {process.ExitCode}.");

            return new StartedProcess(process);
        }

        public async ValueTask DisposeAsync()
        {
            if (!process.HasExited)
            {
                process.Kill(entireProcessTree: true);
                await process.WaitForExitAsync();
            }
            process.Dispose();
        }
    }
}
