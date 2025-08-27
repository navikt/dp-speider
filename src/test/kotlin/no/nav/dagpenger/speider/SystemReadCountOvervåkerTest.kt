package no.nav.dagpenger.speider

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import kotlin.test.Test

class SystemReadCountOvervåkerTest {
    private val testRapid = TestRapid()

    private val overvåker = SystemReadCountOvervåker(testRapid, maxReadCount = -1)

    @Test
    fun `leser`() {
        val message =
            JsonMessage.newMessage(
                map =
                    mapOf(
                        "key" to "value",
                    ),
            )

        testRapid.sendTestMessage(message.toJson())
    }
}
