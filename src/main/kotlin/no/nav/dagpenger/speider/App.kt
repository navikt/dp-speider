package no.nav.dagpenger.speider

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.prometheus.metrics.core.metrics.Gauge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

private val stateGauge =
    Gauge
        .builder()
        .name("dp_app_status")
        .help("Gjeldende status på apps")
        .labelNames("appnavn")
        .register()
private val logger = LoggerFactory.getLogger("no.nav.dagpenger.speider.App")

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val env = System.getenv()

    val props =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env.getValue("KAFKA_BROKERS"))
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env.getValue("KAFKA_TRUSTSTORE_PATH"))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.getValue("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, env.getValue("KAFKA_KEYSTORE_PATH"))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, env.getValue("KAFKA_CREDSTORE_PASSWORD"))
        }
    val adminClient = AdminClient.create(props)
    val pingProducer = KafkaProducer(props, StringSerializer(), StringSerializer())

    val topic = env.getValue("KAFKA_RAPID_TOPIC")
    val partitionsCount =
        adminClient
            .describeTopics(listOf(topic))
            .allTopicNames()
            .get()
            .getValue(topic)
            .partitions()
            .size

    logger.info("$topic consist of $partitionsCount partitions")

    val appStates = AppStates()
    var scheduledPingJob: Job? = null
    var statusPrinterJob: Job? = null

    RapidApplication
        .create(env)
        .apply {
            register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        statusPrinterJob = GlobalScope.launch { printerJob(rapidsConnection, appStates) }
                        scheduledPingJob = GlobalScope.launch { pinger(pingProducer, topic, partitionsCount) }
                    }

                    override fun onShutdown(rapidsConnection: RapidsConnection) {
                        scheduledPingJob?.cancel()
                        statusPrinterJob?.cancel()
                    }
                },
            )

            ApplicationUpRiver(this, appStates)
            ApplicationNotReadyRiver(this, appStates)
            ApplicationPongRiver(this, appStates)
            ApplicationDownRiver(this, appStates)
            ApplicationStopRiver(this, appStates)
            SystemReadCountOvervåker(this, maxReadCount = 10)
        }.start()
}

private fun Boolean.toInt() = if (this) 1 else 0

private suspend fun CoroutineScope.printerJob(
    rapidsConnection: RapidsConnection,
    appStates: AppStates,
) {
    while (isActive) {
        delay(Duration.ofSeconds(15))
        val threshold = LocalDateTime.now().minusMinutes(1)
        logger.info(appStates.reportString(threshold))
        appStates.report(threshold).onEach { (app, state) ->
            stateGauge.labelValues(app).set(state.toInt().toDouble())
        }
        appStates.instances(threshold).also { report ->
            rapidsConnection.publish(
                JsonMessage
                    .newMessage(
                        "app_status",
                        mapOf(
                            "threshold" to threshold,
                            "states" to
                                report.map { (appName, info) ->
                                    mapOf(
                                        "app" to appName,
                                        "state" to info.first.toInt(),
                                        "last_active_time" to info.second,
                                        "instances" to
                                            info.third.map { (instanceId, lastActive, isUp) ->
                                                mapOf(
                                                    "instance" to instanceId,
                                                    "last_active_time" to lastActive,
                                                    "state" to isUp.toInt(),
                                                )
                                            },
                                    )
                                },
                        ),
                    ).toJson(),
            )
        }
    }
}

private suspend fun CoroutineScope.pinger(
    producer: Producer<String, String>,
    topic: String,
    partitionCount: Int,
) {
    while (isActive) {
        delay(Duration.ofSeconds(30))
        val packet = JsonMessage.newMessage("ping")
        packet["ping_time"] = LocalDateTime.now()
        // produces a ping to each partition
        repeat(partitionCount) { partition ->
            producer.send(ProducerRecord(topic, partition, UUID.randomUUID().toString(), packet.toJson()))
        }
        producer.flush()
    }
}
