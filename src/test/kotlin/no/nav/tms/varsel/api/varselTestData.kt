package no.nav.tms.varsel.api

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplicationBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.idporten.sidecar.mock.SecurityLevel
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.varsel.api.varsel.Varsel
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.VarselType
import java.time.ZoneOffset
import java.time.ZonedDateTime


const val eventhandlerTestUrl = "https://test.eventhandler.no"
const val aggregatorTestUrl = "https://aggregator.test"

object VarselTestData {
    fun varsel(
        type: VarselType = VarselType.BESKJED,
        forstBehandlet: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        eventId: String = "12345",
        tekst: String = "tekst",
        link: String = "http://link.no",
        isMasked: Boolean = false,
        sistOppdatert: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        sikkerhetsnivaa: Int = 4,
        aktiv: Boolean = true,
        eksternVarsling: Boolean = false,
        prefererteKanaler: List<String> = emptyList(),
        fristUtløpt: Boolean? = false
    ) = Varsel(
        type = type,
        eventId = eventId,
        forstBehandlet = forstBehandlet,
        tekst = tekst,
        link = link,
        isMasked = isMasked,
        sikkerhetsnivaa = sikkerhetsnivaa,
        sistOppdatert = sistOppdatert,
        aktiv = aktiv,
        eksternVarslingSendt = eksternVarsling,
        eksternVarslingKanaler = prefererteKanaler,
        fristUtløpt = fristUtløpt
    )
}

fun TestApplicationBuilder.mockVarselApi(
    httpClient: HttpClient = HttpClientBuilder.build(),
    corsAllowedOrigins: String = "*.nav.no",
    varselConsumer: VarselConsumer = mockk(relaxed = true),
    authMockInstaller: Application.() -> Unit
) {
    application {
        varselApi(
            corsAllowedOrigins = corsAllowedOrigins,
            httpClient = httpClient,
            varselConsumer = varselConsumer,
            authInstaller = authMockInstaller
        )
    }
}

fun ApplicationTestBuilder.setupEventhandlerService(
    aktiveVarslerFromEventHandler: List<Varsel> = emptyList(),
    inaktiveVarslerFromEventHandler: List<Varsel> = emptyList(),
) {
    externalServices {
        hosts(eventhandlerTestUrl) {
            install(ContentNegotiation) { json() }
            routing {
                get("/fetch/varsel/aktive") {
                    call.request.headers["Authorization"] shouldBe "Bearer handlertoken"
                    call.respond(HttpStatusCode.OK, aktiveVarslerFromEventHandler)
                }

                get("/fetch/varsel/inaktive") {
                    call.request.headers["Authorization"] shouldBe "Bearer handlertoken"
                    call.respond(HttpStatusCode.OK, inaktiveVarslerFromEventHandler)
                }
            }
        }
    }
}

fun ApplicationTestBuilder.setupVarselConsumer(
    tokendingsService: TokendingsService = mockk<TokendingsService>().apply {
        coEvery { exchangeToken(any(), "test:eventhandler") } returns "handlertoken"
        coEvery { exchangeToken(any(), "test:eventaggregator") } returns "aggregatortoken"
    }
) = VarselConsumer(
    client = createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)

    },
    eventHandlerBaseURL = eventhandlerTestUrl,
    eventhandlerClientId = "test:eventhandler",
    tokendingsService = tokendingsService,
    eventAggregatorBaseURL = aggregatorTestUrl,
    eventAggregaorClientId = "test:eventaggregator",

    )

fun installIdportenAuthenticatedMock(
    securityLevel: SecurityLevel,
    authenticated: Boolean = true
): Application.() -> Unit = {
    installMockedAuthenticators {
        installTokenXAuthMock {
            alwaysAuthenticated = false
            setAsDefault = false
        }
        installIdPortenAuthMock {
            alwaysAuthenticated = authenticated
            setAsDefault = true
            staticSecurityLevel = securityLevel
            staticUserPid = "12345"
        }
    }
}

