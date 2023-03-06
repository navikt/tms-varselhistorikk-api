package no.nav.tms.varsel.api

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel
import no.nav.tms.token.support.tokenx.validation.mock.installTokenXAuthMock
import no.nav.tms.varsel.api.varsel.AktiveVarsler
import no.nav.tms.varsel.api.varsel.Beskjed
import no.nav.tms.varsel.api.varsel.InaktivtVarsel
import no.nav.tms.varsel.api.varsel.Innboks
import no.nav.tms.varsel.api.varsel.Oppgave
import no.nav.tms.varsel.api.varsel.Varsel
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.VarselType
import org.junit.jupiter.api.Test

class VarselRoutesTest {

    @Test
    fun `Henter alle inaktiverte varsler`() {
        val beskjed = VarselTestData.beskjed()
        val oppgaver = listOf(
            VarselTestData.oppgave(),
            VarselTestData.oppgave()
        )
        val innbokser = listOf(
            VarselTestData.innboks(),
            VarselTestData.innboks(),
            VarselTestData.innboks()
        )

        val response = testApi(
            beskjederFromEventHandler = listOf(beskjed),
            oppgaverFromEventHandler = oppgaver,
            innbokserFromEventHandler = innbokser,
            securityLevel = SecurityLevel.LEVEL_4
        ) {
            url("inaktive")
            method = HttpMethod.Get
            header("fodselsnummer", "12345678912")
        }

        runBlocking {
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

            inaktiveVarsler.first().apply {
                eventId shouldBe beskjed.eventId
                forstBehandlet shouldBe beskjed.forstBehandlet
                type shouldBe VarselType.BESKJED
                isMasked shouldBe false
                tekst shouldBe beskjed.tekst
            }
        }
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

        val response = testApi(
            aktiveVarslerFromEventHandler = varsler,
            securityLevel = SecurityLevel.LEVEL_4
        ) {
            url("aktive")
            method = HttpMethod.Get
            header("fodselsnummer", "12345678912")
        }

        runBlocking {
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
    fun `Maskerer felter hvis loginLevel er mindre enn sikkerhetsnivaa for inaktive`() {
        val beskjed = VarselTestData.beskjed(sikkerhetsnivaa = 4)
        val oppgave = VarselTestData.oppgave(sikkerhetsnivaa = 3)

        val response = testApi(
            beskjederFromEventHandler = listOf(beskjed),
            oppgaverFromEventHandler = listOf(oppgave),
            securityLevel = SecurityLevel.LEVEL_3
        ) {
            url("inaktive")
            method = HttpMethod.Get
            header("fodselsnummer", "12345678912")
        }

        runBlocking {
            response.status shouldBe HttpStatusCode.OK

            val inaktiveVarsler = Json.decodeFromString<List<InaktivtVarsel>>(response.bodyAsText())
            val beskjedResponse = inaktiveVarsler.first { it.type == VarselType.BESKJED }
            val oppgaveResponse = inaktiveVarsler.first { it.type == VarselType.OPPGAVE }

            beskjedResponse.apply {
                eventId shouldBe beskjedResponse.eventId
                forstBehandlet shouldBe beskjedResponse.forstBehandlet
                type shouldBe VarselType.BESKJED
                isMasked shouldBe true
                tekst shouldBe null
                eksternVarslingSendt shouldBe beskjedResponse.eksternVarslingSendt
                eksternVarslingKanaler shouldBe beskjedResponse.eksternVarslingKanaler
            }

            oppgaveResponse.apply {
                isMasked shouldBe false
                tekst shouldBe oppgaveResponse.tekst
            }
        }
    }

    private fun testApi(
        beskjederFromEventHandler: List<Beskjed> = emptyList(),
        oppgaverFromEventHandler: List<Oppgave> = emptyList(),
        innbokserFromEventHandler: List<Innboks> = emptyList(),
        aktiveVarslerFromEventHandler: List<Varsel> = emptyList(),
        securityLevel: SecurityLevel,
        clientBuilder: HttpRequestBuilder.() -> Unit
    ): HttpResponse {
        val eventhandlerTestUrl = "https://test.eventhandler.no"
        lateinit var response: HttpResponse
        testApplication {
            externalServices {
                hosts(eventhandlerTestUrl) {
                    install(ContentNegotiation) { json() }
                    routing {
                        get("/fetch/beskjed/inaktive") {
                            call.respond(HttpStatusCode.OK, beskjederFromEventHandler)
                        }

                        get("/fetch/oppgave/inaktive") {
                            call.respond(HttpStatusCode.OK, oppgaverFromEventHandler)
                        }

                        get("/fetch/innboks/inaktive") {
                            call.respond(HttpStatusCode.OK, innbokserFromEventHandler)
                        }

                        get("/fetch/varsel/aktive") {
                            call.respond(HttpStatusCode.OK, aktiveVarslerFromEventHandler)
                        }
                    }
                }
            }

            val httpClient = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(jsonConfig())
                }
                install(HttpTimeout)
            }

            val tokendingsMock = mockk<TokendingsService>().apply {
                coEvery { exchangeToken(any(), any()) } returns "<dummytoken>"
            }

            val varselConsumer = VarselConsumer(
                client = httpClient,
                eventHandlerBaseURL = eventhandlerTestUrl,
                eventhandlerClientId = "",
                tokendingsService = tokendingsMock,
            )

            mockVarselbjelleApi(
                varselConsumer = varselConsumer,
                authMockInstaller = installAuthMock(securityLevel)
            )

            response = client.request { clientBuilder() }
        }
        return response
    }
}

fun TestApplicationBuilder.mockVarselbjelleApi(
    httpClient: HttpClient = HttpClientBuilder.build(),
    corsAllowedOrigins: String = "*.nav.no",
    corsAllowedSchemes: String = "https",
    varselConsumer: VarselConsumer = mockk(relaxed = true),
    authMockInstaller: Application.() -> Unit
) {
    application {
        varselApi(
            corsAllowedOrigins = corsAllowedOrigins,
            corsAllowedSchemes = corsAllowedSchemes,
            httpClient = httpClient,
            varselConsumer = varselConsumer,
            authInstaller = authMockInstaller
        )
    }
}

private fun installAuthMock(securityLevel: SecurityLevel): Application.() -> Unit = {
    installTokenXAuthMock {
        alwaysAuthenticated = true
        setAsDefault = true
        staticSecurityLevel = securityLevel
        staticUserPid = "12345"
    }
}