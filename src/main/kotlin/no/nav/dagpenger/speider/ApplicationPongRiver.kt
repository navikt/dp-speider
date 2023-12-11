package no.nav.dagpenger.speider

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.temporal.ChronoUnit

internal class ApplicationPongRiver(
    rapidsConnection: RapidsConnection,
    private val appStates: AppStates,
) : River.PacketListener {
    private val logger = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "pong") }
            validate { it.requireKey("app_name", "instance_id") }
            validate { it.require("ping_time", JsonNode::asLocalDateTime) }
            validate { it.require("pong_time", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
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
    ) {
        logger.error("forstod ikke pong:\n${problems.toExtendedReport()}")
    }
}
