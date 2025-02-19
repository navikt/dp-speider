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

internal class ApplicationNotReadyRiver(
    rapidsConnection: RapidsConnection,
    private val appStates: AppStates,
) : River.PacketListener {
    private val logger = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "application_not_ready") }
            validate { it.requireKey("app_name", "instance_id") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val instance = packet["instance_id"].asText()
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
        metadata: MessageMetadata,
    ) {
        logger.error("forstod ikke application_not_ready:\n${problems.toExtendedReport()}")
    }
}
