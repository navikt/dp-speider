package no.nav.dagpenger.speider

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import java.time.temporal.ChronoUnit

internal class ApplicationPongRiver(
    rapidsConnection: RapidsConnection,
    private val appStates: AppStates,
) : River.PacketListener {
    private val logger = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "pong") }
            validate { it.requireKey("app_name", "instance_id") }
            validate { it.require("ping_time", JsonNode::asLocalDateTime) }
            validate { it.require("pong_time", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val app = packet["app_name"].asText()
        val instance = packet["instance_id"].asText()
        val pingTime = packet["ping_time"].asLocalDateTime()
        val pongTime = packet["pong_time"].asLocalDateTime()

        logger.info(
            "{}-{} svarte på ping etter {} sekunder",
            app,
            instance,
            ChronoUnit.SECONDS.between(pingTime, pongTime),
        )
        appStates.ping(app, instance, pongTime)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error("forstod ikke pong:\n${problems.toExtendedReport()}")
    }
}
