package no.nav.dagpenger.speider

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry

class SystemReadCountOvervåker(
    rapidsConnection: RapidsConnection,
    private val maxReadCount: Int = 30,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition { it.requireKey("system_read_count", "@opprettet") }
            precondition {
                it.require("system_read_count") { readCount ->
                    readCount.asInt() > maxReadCount
                }
            }
            validate { it.interestedIn("@event_name", "system_participating_services", "@id") }
        }
    }

    private val logger = KotlinLogging.logger {}

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.warn {
            """Høy system_read_count oppdaget: ${packet["system_read_count"].asInt()} (max: $maxReadCount)
                |@opprettet: ${packet["@opprettet"].asText()}
                |@event_name: ${packet["@event_name"].asText("ukjent")}
                |@id: ${packet["@id"].asText("ukjent")}
            """.trimMargin()
        }
    }
}
