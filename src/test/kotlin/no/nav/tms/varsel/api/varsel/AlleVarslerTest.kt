package no.nav.tms.varsel.api.varsel

import io.kotest.matchers.shouldBe
import no.nav.tms.varsel.api.varsel.v2.AlleVarsler
import org.junit.jupiter.api.Test

class AlleVarslerTest {
    @Test
    fun `Skal markere maskert varsel i output`() {
        val incomingVarselList = listOf(VarselV2TestData.incomingVarsel(innhold = null),
        VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
        VarselV2TestData.incomingVarsel(type = VarselType.oppgave, innhold = null),
        VarselV2TestData.incomingVarsel())


        AlleVarsler.fromVarsler(incomingVarselList).apply {
            hasMaskedVarsel shouldBe true
            aktive.beskjeder[0].isMasked shouldBe true
            aktive.oppgaver[0].isMasked shouldBe false
            aktive.beskjeder[1].isMasked shouldBe false
        }
    }

    @Test
    fun `Riktig antall varsler i output `() {
        val incomingVarselList = listOf(
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks, aktiv = false)
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
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks, aktiv = false)
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

    @Test
    fun `Ikke sende med lenke på inaktive oppgaver`() {
        val incomingVarselList = listOf(
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed, aktiv = false),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks, aktiv = false)
        )

        AlleVarsler.fromVarsler(incomingVarselList).apply {
            aktive.oppgaver[0].link shouldBe "www.nav.no/test"
            aktive.beskjeder.forEach() {
                it.link shouldBe "www.nav.no/test"
            }
            inaktive.forEach() {
                if(it.type == VarselType.oppgave) {
                    it.link shouldBe null
                } else {
                    it.link shouldBe "www.nav.no/test"
                }
            }

        }
    }

}