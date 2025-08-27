package no.nav.dagpenger.speider

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDateTime

class SystemReadCountOvervåker(
    rapidsConnection: RapidsConnection,
    private val maxReadCount: Int = 30,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireKey("system_read_count", "@opprettet") }
                validate { it.interestedIn("@event_name", "system_participating_services", "@id") }
            }.register(this)
    }

    private val logger = KotlinLogging.logger {}

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val pakka = Pakka(packet)
        if (pakka.readCount > maxReadCount) {
            logger.warn {
                """Høy system_read_count oppdaget: ${packet["system_read_count"].asInt()} (max: $maxReadCount)
                     |Hendelse: ${pakka.eventName}
                     |Opprettet: ${pakka.opprettet}
                     |Systemer involvert og deres read-tidspunkt:
                     |${pakka.apperInvolvert.joinToString(separator = "\n") { (name, time) -> "  - $name: $time" }}
                     |Hendelse-ID: ${packet["@id"].asText()}
                """.trimMargin()
            }
        }
    }
}

typealias App = Pair<String, LocalDateTime>

private data class Pakka(
    private val packet: JsonMessage,
) {
    val readCount: Int = packet["system_read_count"].asInt()
    val eventName: String = packet["@event_name"].asText("ukjent")
    val opprettet: LocalDateTime = packet["@opprettet"].asLocalDateTime()
    val apperInvolvert: List<App> =
        packet["system_participating_services"].map {
            val navn = if (it.has("service")) it["service"].asText() else "ukjent"
            navn to it["time"].asLocalDateTime()
        }
}
