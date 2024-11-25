package no.nav.tms.varsel.api.varsel

import io.kotest.matchers.shouldBe
import no.nav.tms.varsel.api.varsel.v2.AktiveVarslerV2
import no.nav.tms.varsel.api.varsel.v2.AlleVarsler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AktiveVarslerV2Test{
    @Test
    fun `Riktig antall varsler i output`(){
        val incomingVarselList = listOf(
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),

        )

        val result = AktiveVarslerV2.fromVarsler(incomingVarselList)
        assertEquals(3, result.beskjeder.size)
        assertEquals(2, result.oppgaver.size)
    }

    @Test
    fun `Ingen varsler i output`(){
        val incomingVarselList = listOf<VarselAuthority.Varsel>()

        val result = AktiveVarslerV2.fromVarsler(incomingVarselList)
        assertEquals(0, result.beskjeder.size)
        assertEquals(0, result.oppgaver.size)
    }

    @Test
    fun `Kun aktive beskjeder skal være inaktiverbare`() {
        val incomingVarselList = listOf(
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.beskjed),
            VarselV2TestData.incomingVarsel(type = VarselType.innboks),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
            VarselV2TestData.incomingVarsel(type = VarselType.oppgave),
        )

        AktiveVarslerV2.fromVarsler(incomingVarselList).apply {
            beskjeder[0].isInaktiverbar shouldBe true
            beskjeder[1].isInaktiverbar shouldBe true
            beskjeder[2].isInaktiverbar shouldBe false
            oppgaver.forEach() {
                it.isInaktiverbar shouldBe false
            }

        }
    }
}