package io.beatmaps.cloudflare

import com.fasterxml.jackson.module.kotlin.readValue
import io.beatmaps.common.DownloadInfo
import io.beatmaps.common.jackson
import io.beatmaps.common.rabbitHost
import io.ktor.server.application.Application
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.MqttSubscription
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import pl.jutupe.ktor_rabbitmq.RabbitMQ
import pl.jutupe.ktor_rabbitmq.publish

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

    client.setCallback(object : MqttCallback {
        override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
            println("connectionLost")
            disconnectResponse?.exception?.printStackTrace()
        }

        override fun mqttErrorOccurred(exception: MqttException?) {
            println("mqttErrorOccurred")
            exception?.printStackTrace()
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
            println("messageArrived")
            val info = jackson.readValue<DownloadInfo>(message.toString())
            println(info)

            if (rabbitHost.isNotEmpty()) {
                app.attributes[RabbitMQ.RabbitMQKey].publish("beatmaps", "download.hash.${info.hash}", null, info)
            } else {
                println("Skipping publish")
            }
        }

        override fun deliveryComplete(token: IMqttToken?) {
            println("deliveryComplete")
        }

        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            println("connectComplete")
            client.subscribe(arrayOf(MqttSubscription("downloads")))
        }

        override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
            println("authPacketArrived")
        }
    })

    client.connect(connectionOptions)
}