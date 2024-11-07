package no.nav.tms.varsel.api.varsel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AlleVarslerTest {
    @Test
    fun `Skal markere maskert varsel i output`() {
        val incomingVarsel1 = AlleVarslerTestData.incomingVarsel(innhold = null)
        val incomingVarsel2 = AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave)
        val incomingVarsel3 = AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave, innhold = null)
        val incomingVarsel4 = AlleVarslerTestData.incomingVarsel()


        AlleVarsler.fromVarsler(listOf(incomingVarsel1,incomingVarsel2,incomingVarsel3,incomingVarsel4)).apply {
            hasMaskedVarsel shouldBe true
            aktive.beskjeder[0].isMasked shouldBe true
            aktive.oppgaver[0].isMasked shouldBe false
            aktive.beskjeder[1].isMasked shouldBe false
        }
    }

    @Test
    fun `Riktig antall varsler i output `() {
        val incomingVarselList = listOf(
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed),
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed),
            AlleVarslerTestData.incomingVarsel(type = VarselType.innboks),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave),
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.innboks, aktiv = false)
        )

        AlleVarsler.fromVarsler(incomingVarselList).apply {
            aktive.beskjeder.size shouldBe 3
            aktive.oppgaver.size shouldBe 2
            inaktive.size shouldBe 5
        }
    }

    @Test
    fun `Kun aktive og ikke-maskerte beskjeder skal være inaktiverbare`() {
        val incomingVarselList = listOf(
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed),
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed),
            AlleVarslerTestData.incomingVarsel(type = VarselType.innboks),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave),
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            AlleVarslerTestData.incomingVarsel(type = VarselType.innboks, aktiv = false)
        )

        AlleVarsler.fromVarsler(incomingVarselList).apply {
            aktive.beskjeder[0].isInaktiverbar shouldBe true
            aktive.beskjeder[1].isInaktiverbar shouldBe true
            aktive.beskjeder[2].isInaktiverbar shouldBe false
            aktive.oppgaver.forEach() {
                it.isInaktiverbar shouldBe false
            }
            inaktive.forEach() {
                it.isInaktiverbar shouldBe false
            }
        }
    }

}