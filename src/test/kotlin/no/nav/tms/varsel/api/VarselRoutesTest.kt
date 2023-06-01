package no.nav.tms.varsel.api

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokendings.exchange.TokenXHeader
import no.nav.tms.token.support.idporten.sidecar.mock.SecurityLevel as IdportenSecurityLevel
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel as TokenXSecurityLevel
import no.nav.tms.varsel.api.varsel.AktiveVarsler
import no.nav.tms.varsel.api.varsel.AntallVarsler
import no.nav.tms.varsel.api.varsel.InaktivtVarsel
import no.nav.tms.varsel.api.varsel.VarselType
import org.junit.jupiter.api.Test

class VarselRoutesTest {

    private val tokendingsMckk = mockk<TokendingsService>().apply {
        coEvery { exchangeToken(any(), any()) } returns "<dummytoken>"
    }

    @Test
    fun `Henter alle inaktiverte varsler med tokenX autentisering`() {
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.BESKJED),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.INNBOKS)
        )
        //tokenx
        testApplication {
            setupExternalServices(inaktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(tokendingsMckk),
                authMockInstaller = installTokenXAuthenticatedMock(TokenXSecurityLevel.LEVEL_4)
            )

            client.get("/tms-varsel-api/inaktive").status shouldBe HttpStatusCode.Unauthorized
            val response = client.get("/inaktive") {
                header(TokenXHeader.Authorization, "tokenxtoken")
            }
            response.status shouldBe HttpStatusCode.OK

        }

    }

    @Test
    fun `Henter inaktiverte varsel med idporten autentisering`() {

        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.BESKJED),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.INNBOKS),
            VarselTestData.varsel(type = VarselType.INNBOKS),
            VarselTestData.varsel(type = VarselType.INNBOKS),
        )

        testApplication {
            setupExternalServices(inaktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(tokendingsMckk),
                authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4)
            )

            client.get("/tms-varsel-api/inaktive") {
                header(TokenXHeader.Authorization, "tokenxtoken")
            }.status shouldBe HttpStatusCode.Unauthorized

            val response = client.get("/inaktive")
            response.status shouldBe HttpStatusCode.OK
            response.status shouldBe HttpStatusCode.OK

            val inaktiveVarsler = Json.decodeFromString<List<InaktivtVarsel>>(response.bodyAsText())
            inaktiveVarsler.size shouldBe 6
            inaktiveVarsler.map { it.type } shouldContainExactlyInAnyOrder listOf(
                VarselType.BESKJED,
                VarselType.OPPGAVE,
                VarselType.OPPGAVE,
                VarselType.INNBOKS,
                VarselType.INNBOKS,
                VarselType.INNBOKS
            )

            val beskjed = varsler.first { it.type == VarselType.BESKJED }
            inaktiveVarsler.first { it.type == VarselType.BESKJED }.apply {
                eventId shouldBe beskjed.eventId
                forstBehandlet shouldBe beskjed.forstBehandlet
                type shouldBe VarselType.BESKJED
                isMasked shouldBe false
                tekst shouldBe beskjed.tekst
            }

        }
    }

    @Test
    fun `Metaroutes reroutes ikke`() =
        testApplication {
            setupExternalServices(inaktiveVarslerFromEventHandler = listOf())
            mockVarselApi(
                varselConsumer = setupVarselConsumer(tokendingsMckk),
                authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4, false)
            )

            client.get("/tms-varsel-api/internal/isAlive").status shouldBe HttpStatusCode.OK
            client.get("/tms-varsel-api/internal/isReady").status shouldBe HttpStatusCode.OK
        }


    @Test
    fun `Henter alle aktive varsler`() {
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.BESKJED),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.INNBOKS),
            VarselTestData.varsel(type = VarselType.INNBOKS),
            VarselTestData.varsel(type = VarselType.INNBOKS),
        )

        testApplication {
            setupExternalServices(aktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4)
            )

            client.get("/tms-varsel-api/aktive") {
                header(
                    TokenXHeader.Authorization,
                    "tokenxtoken"
                )
            }.status shouldBe HttpStatusCode.Unauthorized

            val response = client.get("/tms-varsel-api/aktive")
            response.status shouldBe HttpStatusCode.OK

            val aktiveVarsler = Json.decodeFromString<AktiveVarsler>(response.bodyAsText())
            aktiveVarsler.beskjeder.size shouldBe 1
            aktiveVarsler.oppgaver.size shouldBe 2
            aktiveVarsler.innbokser.size shouldBe 3

            val beskjed = varsler.first { it.type == VarselType.BESKJED }
            aktiveVarsler.beskjeder.first().apply {
                eventId shouldBe beskjed.eventId
                forstBehandlet shouldBe beskjed.forstBehandlet
                isMasked shouldBe beskjed.isMasked
                tekst shouldBe beskjed.tekst
                link shouldBe beskjed.link
                eksternVarslingSendt shouldBe beskjed.eksternVarslingSendt
                eksternVarslingKanaler shouldBe beskjed.eksternVarslingKanaler
            }
        }

    }

    @Test
    fun `Henter antall aktive varsler`() {
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.BESKJED),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.OPPGAVE),
            VarselTestData.varsel(type = VarselType.INNBOKS),
            VarselTestData.varsel(type = VarselType.INNBOKS),
            VarselTestData.varsel(type = VarselType.INNBOKS),
        )
        testApplication {
            setupExternalServices(aktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4)
            )

            val response = client.get("/tms-varsel-api/antall/aktive")

            response.status shouldBe HttpStatusCode.OK

            val antallVarsler = Json.decodeFromString<AntallVarsler>(response.bodyAsText())
            antallVarsler.beskjeder shouldBe 1
            antallVarsler.oppgaver shouldBe 2
            antallVarsler.innbokser shouldBe 3
        }

        testApplication {
            setupExternalServices(aktiveVarslerFromEventHandler = varsler)
        }


    }
}

private fun installTokenXAuthenticatedMock(securityLevel: TokenXSecurityLevel): Application.() -> Unit = {
    installMockedAuthenticators {
        installTokenXAuthMock {
            alwaysAuthenticated = true
            setAsDefault = false
            staticSecurityLevel = securityLevel
            staticUserPid = "12345"
        }
        installIdPortenAuthMock {
            alwaysAuthenticated = false
            setAsDefault = true
        }
    }
}


