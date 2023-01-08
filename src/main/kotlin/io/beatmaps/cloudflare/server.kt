package io.beatmaps.cloudflare

import io.beatmaps.common.db.setupDB
import io.beatmaps.common.rabbitHost
import io.beatmaps.common.setupAMQP
import io.beatmaps.common.setupLogging
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pl.jutupe.ktor_rabbitmq.RabbitMQ

val port = System.getenv("LISTEN_PORT")?.toIntOrNull() ?: 3031
val host = System.getenv("LISTEN_HOST") ?: "127.0.0.1"

fun main() {
    setupLogging()
    setupDB()

    embeddedServer(Netty, port = port, host = host, module = Application::cloudflare).start(wait = true)
}

fun Application.cloudflare() {
    if (rabbitHost.isNotEmpty()) {
        install(RabbitMQ) {
            setupAMQP()
        }
    }

    pubsubListen(this)
}
