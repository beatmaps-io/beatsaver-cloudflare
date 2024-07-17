package io.beatmaps.cloudflare

import io.ktor.server.application.Application
import org.eclipse.paho.mqttv5.client.MqttClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions

val MQTT_HOST = System.getenv("MQTT_HOST") ?: "beatsaver.beatsaver.cloudflarepubsub.com"
val MQTT_PORT = System.getenv("MQTT_PORT") ?: "8883"
val MQTT_PASS = System.getenv("MQTT_PASS") ?: "insecure-password"
val MQTT_CLIENTID = System.getenv("MQTT_CLIENTID") ?: "beatmaps"

fun pubsubListen(app: Application) {
    val uri = "ssl://${MQTT_HOST}:${MQTT_PORT}"

    val connectionOptions = MqttConnectionOptions().apply {
        serverURIs = arrayOf(uri)
        password = MQTT_PASS.toByteArray()
        isCleanStart = false
        isAutomaticReconnect = true
    }

    val client = MqttClient(uri, MQTT_CLIENTID)
    client.setCallback(BMCallback(app, client))
    client.connect(connectionOptions)
}
