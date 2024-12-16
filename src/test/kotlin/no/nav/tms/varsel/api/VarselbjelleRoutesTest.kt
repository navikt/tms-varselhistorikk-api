package no.nav.tms.varsel.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import no.nav.tms.varsel.api.varsel.VarselType
import no.nav.tms.varsel.api.varsel.VarselbjelleVarsler
import org.junit.jupiter.api.Test

class VarselbjelleRoutesTest {

    private val objectMapper = jacksonObjectMapper().jsonConfig()

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
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.HIGH)
            )

            client.get("/varselbjelle/varsler").apply {
                status shouldBe HttpStatusCode.OK
                val varselbjellevarsler: VarselbjelleVarsler = bodyFromJson()
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
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.HIGH)
            )

            client.get("/bjellevarsler").apply {
                status shouldBe HttpStatusCode.OK
                val varselbjellevarsler: VarselbjelleVarsler = bodyFromJson()
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
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.HIGH)
            )

            client.get("/bjellevarsler?preferert_spraak=en").apply {
                status shouldBe HttpStatusCode.OK
            }

            client.get("/varselbjelle/varsler?preferert_spraak=en").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    private suspend inline fun <reified T> HttpResponse.bodyFromJson(): T = objectMapper.readValue(bodyAsText())
}
