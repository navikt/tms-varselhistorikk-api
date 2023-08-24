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
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import no.nav.tms.token.support.idporten.sidecar.mock.installIdPortenAuthMock
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.varsel.api.varsel.Varsel
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.VarselInnhold
import no.nav.tms.varsel.api.varsel.VarselType
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*


const val varselAuthorityTestUrl = "https://varsel-authority.test"

object VarselTestData {
    fun varsel(
        type: VarselType = VarselType.beskjed,
        opprettet: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        aktivFremTil: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).plusDays(7),
        varselId: String = UUID.randomUUID().toString(),
        tekst: String = "tekst",
        link: String = "http://link.no",
        isMasked: Boolean = false,
        aktiv: Boolean = true,
        inaktivert: ZonedDateTime? = null,
        eksternVarsling: Boolean = false,
        eksernVarslingKanaler: List<String> = emptyList()
    ) = Varsel(
        type = type,
        varselId = varselId,
        opprettet = opprettet,
        aktivFremTil = aktivFremTil,
        innhold = if (isMasked) null else VarselInnhold(tekst, link),
        aktiv = aktiv,
        inaktivert = inaktivert,
        eksternVarslingSendt = eksternVarsling,
        eksternVarslingKanaler = eksernVarslingKanaler
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


fun ApplicationTestBuilder.setupEventhandlerService(vararg varsler: Varsel) = setupEventhandlerService(
    aktiveVarslerFromEventHandler = varsler.toList()
)

fun ApplicationTestBuilder.setupEventhandlerService(
    aktiveVarslerFromEventHandler: List<Varsel> = emptyList(),
    inaktiveVarslerFromEventHandler: List<Varsel> = emptyList(),
) {
    externalServices {
        hosts(varselAuthorityTestUrl) {
            install(ContentNegotiation) { json() }
            routing {
                get("/varsel/sammendrag/aktive") {
                    call.request.headers["Authorization"] shouldBe "Bearer authorityToken"
                    call.respond(HttpStatusCode.OK, aktiveVarslerFromEventHandler)
                }

                get("/varsel/sammendrag/inaktive") {
                    call.request.headers["Authorization"] shouldBe "Bearer authorityToken"
                    call.respond(HttpStatusCode.OK, inaktiveVarslerFromEventHandler)
                }
            }
        }
    }
}

fun ApplicationTestBuilder.setupVarselConsumer(
    tokendingsService: TokendingsService = mockk<TokendingsService>().apply {
        coEvery { exchangeToken(any(), "test:varsel-authority") } returns "authorityToken"
    }
) = VarselConsumer(
    client = createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)

    },
    varselAuthorityUrl = varselAuthorityTestUrl,
    varselAuthorityClientId = "test:varsel-authority",
    tokendingsService = tokendingsService,

    )

fun installIdportenAuthenticatedMock(
    levelOfAssurance: LevelOfAssurance,
    authenticated: Boolean = true
): Application.() -> Unit = {
    installIdPortenAuthMock {
        alwaysAuthenticated = authenticated
        setAsDefault = true
        staticLevelOfAssurance = levelOfAssurance
        staticUserPid = "12345"
    }
}

