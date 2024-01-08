package no.nav.tms.varsel.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import no.nav.tms.varsel.api.varsel.VarselType
import no.nav.tms.varsel.api.varsel.VarselbjelleVarsler
import org.junit.jupiter.api.Test

class VarselbjelleRoutesTest {

    @Test
    fun `Varsel har riktige felter`() = testApplication {
        val expectedBeskjed = VarselTestData.varsel(type = VarselType.beskjed)
        val varsler = listOf(
            expectedBeskjed,
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks)
        )
        testApplication {
            setupVarselAuthority(aktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
            )

            client.get("/varselbjelle/varsler").apply {
                status shouldBe HttpStatusCode.OK
                val varselbjellevarsler = Json.decodeFromString<VarselbjelleVarsler>(bodyAsText())
                varselbjellevarsler.beskjeder.size shouldBe 2
                varselbjellevarsler.oppgaver.size shouldBe 3
                val beskjed = varselbjellevarsler.beskjeder.first { it.varselId == expectedBeskjed.varselId }
                beskjed.varselId shouldBe expectedBeskjed.varselId
                beskjed.eventId shouldBe expectedBeskjed.varselId
                beskjed.isMasked shouldBe (expectedBeskjed.innhold == null)
                beskjed.link shouldBe expectedBeskjed.innhold?.link
                beskjed.spraakkode shouldBe expectedBeskjed.innhold?.spraakkode
                beskjed.tekst shouldBe expectedBeskjed.innhold?.tekst
                beskjed.eksternVarslingKanaler shouldBe expectedBeskjed.eksternVarslingKanaler
                beskjed.tidspunkt shouldBe expectedBeskjed.opprettet
                beskjed.eksternVarslingSendt shouldBe expectedBeskjed.eksternVarslingSendt
                beskjed.type shouldBe "beskjed"
            }
        }
    }

    @Test
    fun `Varsel har riktige felter fra bjellevarsler`() = testApplication {
        val expectedBeskjed = VarselTestData.varsel(type = VarselType.beskjed)
        val varsler = listOf(
            expectedBeskjed,
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks)
        )
        testApplication {
            setupVarselAuthority(aktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
            )

            client.get("/bjellevarsler").apply {
                status shouldBe HttpStatusCode.OK
                val varselbjellevarsler = Json.decodeFromString<VarselbjelleVarsler>(bodyAsText())
                varselbjellevarsler.beskjeder.size shouldBe 2
                varselbjellevarsler.oppgaver.size shouldBe 3
                val beskjed = varselbjellevarsler.beskjeder.first { it.eventId == expectedBeskjed.varselId }
                beskjed.varselId shouldBe expectedBeskjed.varselId
                beskjed.eventId shouldBe expectedBeskjed.varselId
                beskjed.isMasked shouldBe (expectedBeskjed.innhold == null)
                beskjed.link shouldBe expectedBeskjed.innhold?.link
                beskjed.spraakkode shouldBe expectedBeskjed.innhold?.spraakkode
                beskjed.tekst shouldBe expectedBeskjed.innhold?.tekst
                beskjed.eksternVarslingKanaler shouldBe expectedBeskjed.eksternVarslingKanaler
                beskjed.tidspunkt shouldBe expectedBeskjed.opprettet
                beskjed.eksternVarslingSendt shouldBe expectedBeskjed.eksternVarslingSendt
                beskjed.type shouldBe "beskjed"
            }
        }
    }

    @Test
    fun `bruker preferert spraak i kall til authority`() {
        testApplication {
            setupVarselAuthority(expectedSpraakkodeParam = "en")
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
            )

            client.get("/bjellevarsler?preferert_spraak=en").apply {
                status shouldBe HttpStatusCode.OK
            }

            client.get("/varselbjelle/varsler?preferert_spraak=en").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }
}
