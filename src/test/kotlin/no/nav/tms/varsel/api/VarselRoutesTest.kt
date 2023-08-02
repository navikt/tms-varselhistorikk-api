package no.nav.tms.varsel.api

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.tms.token.support.authentication.installer.mock.installMockedAuthenticators
import no.nav.tms.token.support.tokendings.exchange.TokenXHeader
import no.nav.tms.token.support.idporten.sidecar.mock.SecurityLevel as IdportenSecurityLevel
import no.nav.tms.token.support.tokenx.validation.mock.SecurityLevel as TokenXSecurityLevel
import no.nav.tms.varsel.api.varsel.AktiveVarsler
import no.nav.tms.varsel.api.varsel.AntallVarsler
import no.nav.tms.varsel.api.varsel.InaktivtVarsel
import no.nav.tms.varsel.api.varsel.VarselType
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class VarselRoutesTest {

    @Test
    fun `Henter alle inaktiverte varsler med tokenX autentisering`() {
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks)
        )
        //tokenx
        testApplication {
            setupEventhandlerService(inaktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installTokenXAuthenticatedMock(TokenXSecurityLevel.LEVEL_4)
            )

            client.get("/tms-varsel-api/inaktive").status shouldBe HttpStatusCode.Unauthorized
            val response = client.get("/inaktive") {
                header(TokenXHeader.Authorization, "tokenxtoken")
            }
            response.status shouldBe HttpStatusCode.OK
            client.get("tms-varsel-api/metrics").also {
                it.status shouldBe HttpStatusCode.OK
                it.bodyAsText() shouldContain ("tms_api_call")
            }

        }

    }

    @Test
    fun `Henter inaktiverte varsel med idporten autentisering`() {

        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        testApplication {
            setupEventhandlerService(inaktiveVarslerFromEventHandler = varsler)
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
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
                VarselType.beskjed,
                VarselType.oppgave,
                VarselType.oppgave,
                VarselType.innboks,
                VarselType.innboks,
                VarselType.innboks
            )

            val beskjed = varsler.first { it.type == VarselType.beskjed }
            inaktiveVarsler.first { it.type == VarselType.beskjed }.apply {
                varselId shouldBe beskjed.varselId
                eventId shouldBe beskjed.varselId
                tidspunkt shouldBe beskjed.opprettet
                forstBehandlet shouldBe beskjed.opprettet
                type shouldBe VarselType.beskjed
                isMasked shouldBe false
                tekst shouldBe beskjed.innhold?.tekst
            }

        }
    }

    @Test
    fun `Metaroutes reroutes ikke`() =
        testApplication {
            setupEventhandlerService(inaktiveVarslerFromEventHandler = listOf())
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4, false)
            )

            client.get("/tms-varsel-api/internal/isAlive").status shouldBe HttpStatusCode.OK
            client.get("/tms-varsel-api/internal/isReady").status shouldBe HttpStatusCode.OK
        }


    @Test
    fun `Henter alle aktive varsler`() {
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        testApplication {
            setupEventhandlerService(aktiveVarslerFromEventHandler = varsler)
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

            val beskjed = varsler.first { it.type == VarselType.beskjed }
            aktiveVarsler.beskjeder.first().apply {
                varselId shouldBe beskjed.varselId
                eventId shouldBe beskjed.varselId
                tidspunkt shouldBe beskjed.opprettet
                forstBehandlet shouldBe beskjed.opprettet
                isMasked shouldBe (beskjed.innhold == null)
                tekst shouldBe beskjed.innhold?.tekst
                link shouldBe beskjed.innhold?.link
                eksternVarslingSendt shouldBe beskjed.eksternVarslingSendt
                eksternVarslingKanaler shouldBe beskjed.eksternVarslingKanaler
            }
        }
    }

    @Test
    fun `Henter aktive varsler for nivå 3`() = testApplication {
        setupEventhandlerService(VarselTestData.varsel(type = VarselType.beskjed, isMasked = true),
            VarselTestData.varsel(type = VarselType.oppgave, isMasked = true)
        )
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_3)
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
        aktiveVarsler.oppgaver.size shouldBe 1
        aktiveVarsler.innbokser.size shouldBe 0

        (aktiveVarsler.beskjeder+ aktiveVarsler.oppgaver).forEach {
            it.isMasked shouldBe true
            it.tekst shouldBe null
            it.link shouldBe null
        }

    }

    @Test
    fun `Henter antall aktive varsler`() {
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )
        testApplication {
            setupEventhandlerService(aktiveVarslerFromEventHandler = varsler)
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
            setupEventhandlerService(aktiveVarslerFromEventHandler = varsler)
        }


    }

    @Test
    fun `markerer varsler som lest`() = testApplication {
        val expeectedEventId = "hhuu33-91sdf-shdkfh"
        var postCount = 0
        externalServices {
            hosts(varselAuthorityTestUrl) {
                install(ContentNegotiation) { json() }
                routing {
                    post("beskjed/inaktiver") {
                        Json.parseToJsonElement(call.receiveText()).apply {
                            this.jsonObject["varselId"]?.jsonPrimitive?.content shouldBe expeectedEventId
                        }
                        call.request.headers["Authorization"] shouldBe "Bearer authorityToken"
                        postCount++
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4)
        )
        client.post("/tms-varsel-api/beskjed/inaktiver") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"varselId": "$expeectedEventId"}""")
        }.status shouldBe HttpStatusCode.OK

        postCount shouldBe 1
    }

    @Test
    fun `er bakoverkompatibel med fronted ved henting av varsler`() {
        val beskjed = VarselTestData.varsel(type = VarselType.beskjed)
        val oppgave = VarselTestData.varsel(type = VarselType.oppgave, aktiv = false, inaktivert = ZonedDateTime.now())

        testApplication {

            setupEventhandlerService(aktiveVarslerFromEventHandler = listOf(beskjed), inaktiveVarslerFromEventHandler = listOf(oppgave))
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4)
            )

            val aktiveVarsler: AktiveVarsler = client.get("/tms-varsel-api/aktive").fromJson()
            aktiveVarsler.beskjeder.size shouldBe 1
            aktiveVarsler.oppgaver.size shouldBe 0
            aktiveVarsler.innbokser.size shouldBe 0

            aktiveVarsler.beskjeder.first().apply {
                eventId shouldBe beskjed.varselId
                forstBehandlet shouldBe beskjed.opprettet
                isMasked shouldBe (beskjed.innhold == null)
                tekst shouldBe beskjed.innhold?.tekst
                link shouldBe beskjed.innhold?.link
                eksternVarslingSendt shouldBe beskjed.eksternVarslingSendt
                eksternVarslingKanaler shouldBe beskjed.eksternVarslingKanaler
            }

            val inaktiveVarsler: List<InaktivtVarsel> = client.get("/tms-varsel-api/inaktive").fromJson()
            inaktiveVarsler.size shouldBe 1

            inaktiveVarsler.first().apply {
                type shouldBe VarselType.oppgave
                eventId shouldBe oppgave.varselId
                forstBehandlet shouldBe oppgave.opprettet
                isMasked shouldBe (oppgave.innhold == null)
                tekst shouldBe oppgave.innhold?.tekst
                eksternVarslingSendt shouldBe oppgave.eksternVarslingSendt
                eksternVarslingKanaler shouldBe oppgave.eksternVarslingKanaler
            }
        }
    }


    @Test
    fun `er bakoverkompatibel med frontend ved inaktivering av beskjed`() = testApplication {
        val expeectedEventId = "hhuu33-91sdf-shdkfh"
        var postCount = 0
        externalServices {
            hosts(varselAuthorityTestUrl) {
                install(ContentNegotiation) { json() }
                routing {
                    post("beskjed/inaktiver") {
                        Json.parseToJsonElement(call.receiveText()).apply {
                            this.jsonObject["varselId"]?.jsonPrimitive?.content shouldBe expeectedEventId
                        }
                        call.request.headers["Authorization"] shouldBe "Bearer authorityToken"
                        postCount++
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installIdportenAuthenticatedMock(IdportenSecurityLevel.LEVEL_4)
        )
        client.post("/tms-varsel-api/beskjed/inaktiver") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"eventId": "$expeectedEventId"}""")
        }.status shouldBe HttpStatusCode.OK

        postCount shouldBe 1
    }
}

private suspend inline fun <reified T> HttpResponse.fromJson(): T {
    return bodyAsText().let(Json::decodeFromString)
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

