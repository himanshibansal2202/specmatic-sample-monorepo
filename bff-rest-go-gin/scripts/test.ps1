$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not $env:SUT_PORT) { $env:SUT_PORT = "8080" }
if (-not $env:SUT_BASE_URL) { $env:SUT_BASE_URL = "http://localhost:$($env:SUT_PORT)" }
if (-not $env:STUB_BASE_URL) { $env:STUB_BASE_URL = "http://localhost:8090" }
if (-not $env:KAFKA_HOST) { $env:KAFKA_HOST = "localhost" }
if (-not $env:KAFKA_PORT) { $env:KAFKA_PORT = "9092" }
if (-not $env:KAFKA_BROKER_URL) { $env:KAFKA_BROKER_URL = "$($env:KAFKA_HOST):$($env:KAFKA_PORT)" }
if (-not $env:SPECMATIC_VERSION) { $env:SPECMATIC_VERSION = "1.19.1" }
if (-not $env:SPECMATIC_JAR) { $env:SPECMATIC_JAR = Join-Path $Root "build/specmatic/specmatic-enterprise-$($env:SPECMATIC_VERSION).jar" }

New-Item -ItemType Directory -Force -Path (Join-Path $Root "build/specmatic") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $Root "build/reports/specmatic") | Out-Null

if (-not (Test-Path $env:SPECMATIC_JAR)) {
  mvn -q dependency:copy "-Dartifact=io.specmatic.enterprise:executable-all:$($env:SPECMATIC_VERSION)" "-DoutputDirectory=$(Join-Path $Root 'build/specmatic')" "-Dmdep.stripVersion=false"
  Move-Item -Force (Join-Path $Root "build/specmatic/executable-all-$($env:SPECMATIC_VERSION).jar") $env:SPECMATIC_JAR
}

go test ./...
go build -o (Join-Path $Root "build/bff-rest-go-gin.exe") ./cmd/server

$proc = Start-Process -FilePath (Join-Path $Root "build/bff-rest-go-gin.exe") -PassThru
try {
  for ($i = 0; $i -lt 60; $i++) {
    try {
      Invoke-WebRequest "$($env:SUT_BASE_URL)/monitor/1" -UseBasicParsing | Out-Null
      break
    } catch {
      Start-Sleep -Seconds 1
    }
  }
  java -jar $env:SPECMATIC_JAR run-suite --config specmatic.yaml
} finally {
  Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}
