package io.specmatic.samples.store

data class AppConfig(
    val grpcPort: Int = envInt("SUT_PORT", 8080),
    val managementPort: Int = envInt("MANAGEMENT_PORT", 8081),
)

private fun envInt(name: String, defaultValue: Int): Int =
    System.getenv(name)?.toIntOrNull() ?: System.getProperty(name)?.toIntOrNull() ?: defaultValue
