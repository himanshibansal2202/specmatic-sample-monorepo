package io.specmatic.samples.store

fun main() {
    val app = StoreApplication(AppConfig())
    Runtime.getRuntime().addShutdownHook(Thread { app.stop() })
    app.start()
    Thread.currentThread().join()
}
