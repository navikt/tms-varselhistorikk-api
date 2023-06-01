package no.nav.tms.varsel.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.tms.token.support.idporten.sidecar.mock.SecurityLevel
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.varsel.api.varsel.VarselType
import no.nav.tms.varsel.api.varsel.VarselbjelleVarsel
import no.nav.tms.varsel.api.varsel.VarselbjelleVarsler
import org.junit.jupiter.api.Test

class VarselbjelleRoutesTest {

    private val tokendingsMckk = mockk<TokendingsService>().apply {
        coEvery { exchangeToken(any(), any()) } returns "<dummytoken>"
    }

    @Test
    fun `Varsel har riktige felter`() = testApplication {
        val expectedBeskjed = VarselTestData.varsel(type = VarselType.BESKJED)
        val varsler = listOf(
            expectedBeskjed,
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.INNBOKS)
        )
        testApplication {
            setupExternalServices(aktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(tokendingsMckk),
                authMockInstaller = installIdportenAuthenticatedMock(SecurityLevel.LEVEL_4)
            )

            client.get("/tms-varsel-api/varselbjelle/varsler").apply {
                status shouldBe HttpStatusCode.OK
                val varselbjellevarsler = Json.decodeFromString<VarselbjelleVarsler>(bodyAsText())
                varselbjellevarsler.beskjeder.size shouldBe 2
                varselbjellevarsler.oppgaver.size shouldBe 3
                val beskjed = varselbjellevarsler.beskjeder.first { it.eventId == expectedBeskjed.eventId }
                beskjed.eventId shouldBe expectedBeskjed.eventId
                beskjed.link shouldBe expectedBeskjed.link
                beskjed.tekst shouldBe expectedBeskjed.tekst
                beskjed.isMasked shouldBe expectedBeskjed.isMasked
                beskjed.eksternVarslingKanaler shouldBe expectedBeskjed.eksternVarslingKanaler
                beskjed.tidspunkt shouldBe expectedBeskjed.forstBehandlet
                beskjed.eksternVarslingSendt shouldBe expectedBeskjed.eksternVarslingSendt
                beskjed.type shouldBe "BESKJED"
            }
        }
    }
}