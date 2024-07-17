package io.beatmaps.cloudflare

import io.beatmaps.common.DownloadInfo
import io.beatmaps.common.json
import io.beatmaps.common.rabbitOptional
import io.ktor.server.application.Application
import mu.KLogging
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttClient
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.MqttSubscription
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import pl.jutupe.ktor_rabbitmq.publish

class BMCallback(private val app: Application, private val client: MqttClient) : MqttCallback {
    override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
        logger.error(disconnectResponse?.exception) { "connectionLost" }
    }

    override fun mqttErrorOccurred(exception: MqttException?) {
        logger.error(exception) { "mqttErrorOccurred" }
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        logger.debug { "messageArrived" }
        val info = json.decodeFromString<DownloadInfo>(message.toString())

        app.rabbitOptional {
            publish("beatmaps", "download.hash.${info.hash}", null, info)
        }
    }

    override fun deliveryComplete(token: IMqttToken?) {
        logger.debug { "deliveryComplete" }
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        logger.info { "connectComplete" }
        client.subscribe(arrayOf(MqttSubscription("downloads")))
    }

    override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
        logger.info { "authPacketArrived" }
    }

    companion object : KLogging()
}
