package no.nav.dagpenger.speider

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertTrue

class ApplicationNotReadyRiverTest {
    private val appStates = AppStates()
    private val testRapid =
        TestRapid().also { rapidsConnection ->
            ApplicationNotReadyRiver(rapidsConnection, appStates)
        }

    @Test
    fun `skal lese 'application_not_ready' eventer`() {
        testRapid.sendTestMessage(
            """
            {
              "@event_name": "application_not_ready",
              "app_name": "my-app",
              "instance_id": "instance1",
              "@opprettet": "${LocalDateTime.now()}"
            }
            """.trimIndent(),
        )
        appStates.report(LocalDateTime.now()).also {
            assertTrue { it.size == 1 }
            assertTrue(it["my-app"] == false)
        }
    }
}
