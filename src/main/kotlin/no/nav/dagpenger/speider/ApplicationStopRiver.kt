package no.nav.dagpenger.speider

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry

internal class ApplicationStopRiver(
    rapidsConnection: RapidsConnection,
    private val appStates: AppStates,
) : River.PacketListener {
    private val logger = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "application_stop") }
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
        appStates.down(
            packet["app_name"].asText(),
            packet["instance_id"].asText(),
            packet["@opprettet"].asLocalDateTime(),
        )
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error("forstod ikke application_stop:\n${problems.toExtendedReport()}")
    }
}
