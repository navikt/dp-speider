package no.nav.dagpenger.speider

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ApplicationNotReadyRiver(
    rapidsConnection: RapidsConnection,
    private val appStates: AppStates,
) : River.PacketListener {
    private val logger = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "application_not_ready") }
            validate { it.requireKey("app_name", "instance_id") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val instance = packet["@instance_id"].asText()
        val app = packet["app_name"].asText()
        logger.info { "application_not_ready: $app - $instance" }
        appStates.down(
            app,
            instance,
            packet["@opprettet"].asLocalDateTime(),
        )
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        logger.error("forstod ikke application_not_ready:\n${problems.toExtendedReport()}")
    }
}
