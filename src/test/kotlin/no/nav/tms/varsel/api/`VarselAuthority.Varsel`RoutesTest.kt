package no.nav.tms.varsel.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.*
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tms.varsel.api.varsel.*

class VarselRoutesTest {

    private val objectMapper = jacksonObjectMapper().jsonConfig()

    @Test
    fun `Henter alle varsler, inkative og aktive`() = varselRoutesTest{ client ->
        val aktiveVarsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )
        val inaktivtVarsel = listOf(
            VarselTestData.varsel(type = VarselType.beskjed, aktiv = false),
            VarselTestData.varsel(type = VarselType.beskjed, aktiv = false),
            VarselTestData.varsel(type = VarselType.beskjed, aktiv = false),
            VarselTestData.varsel(type = VarselType.oppgave, aktiv = false),
            VarselTestData.varsel(type = VarselType.innboks, aktiv = false),
            VarselTestData.varsel(type = VarselType.innboks,aktiv = false),
        )
        setupVarselAuthority(inaktiveVarslerFromEventHandler = inaktivtVarsel, aktiveVarslerFromEventHandler = aktiveVarsler)

        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/alle")
        response.status shouldBe HttpStatusCode.OK

        val alleVarsler: AlleVarsler = response.bodyFromJson()
        alleVarsler.aktive.beskjeder.size shouldBe 4
        alleVarsler.aktive.oppgaver.size shouldBe 2
        alleVarsler.inaktive.size shouldBe 6

    }

    @Test
    fun `Henter inaktiverte varsler`() = varselRoutesTest { client ->
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        setupVarselAuthority(inaktiveVarslerFromEventHandler = varsler)
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/inaktive")
        response.status shouldBe HttpStatusCode.OK
        response.status shouldBe HttpStatusCode.OK

        val inaktiveVarsler: List<InaktivtVarsel> = response.bodyFromJson()
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

    @Test
    fun `Henter alle aktive varsler`() = varselRoutesTest { client ->
        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        setupVarselAuthority(aktiveVarslerFromEventHandler = varsler)
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/aktive")
        response.status shouldBe HttpStatusCode.OK

        val aktiveVarsler: AktiveVarsler = response.bodyFromJson()

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

    @Test
    fun `Henter aktive varsler for nivå 3`() = varselRoutesTest { client ->
        setupVarselAuthority(VarselTestData.varsel(type = VarselType.beskjed, isMasked = true),
            VarselTestData.varsel(type = VarselType.oppgave, isMasked = true)
        )
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_3)
        )

        val response = client.get("/aktive")
        response.status shouldBe HttpStatusCode.OK

        val aktiveVarsler: AktiveVarsler = response.bodyFromJson()

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
    fun `Henter antall aktive varsler`() = varselRoutesTest { client ->

        val varsler = listOf(
            VarselTestData.varsel(type = VarselType.beskjed),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.oppgave),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
            VarselTestData.varsel(type = VarselType.innboks),
        )

        setupVarselAuthority(aktiveVarslerFromEventHandler = varsler)
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val response = client.get("/antall/aktive")

        response.status shouldBe HttpStatusCode.OK

        val antallVarsler: AntallVarsler = response.bodyFromJson()
        antallVarsler.beskjeder shouldBe 1
        antallVarsler.oppgaver shouldBe 2
        antallVarsler.innbokser shouldBe 3
    }

    @Test
    fun `markerer varsler som lest`() = testApplication {
        val expeectedEventId = "hhuu33-91sdf-shdkfh"
        var postCount = 0
        externalServices {
            hosts(varselAuthorityTestUrl) {
                install(ContentNegotiation) { jackson() }
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
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )
        client.post("/beskjed/inaktiver") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"varselId": "$expeectedEventId"}""")
        }.status shouldBe HttpStatusCode.OK

        postCount shouldBe 1
    }

    @Test
    fun `er bakoverkompatibel med fronted ved henting av varsler`() = varselRoutesTest { client ->

        val beskjed = VarselTestData.varsel(type = VarselType.beskjed)
        val oppgave = VarselTestData.varsel(type = VarselType.oppgave, aktiv = false, inaktivert = ZonedDateTime.now())

        setupVarselAuthority(aktiveVarslerFromEventHandler = listOf(beskjed), inaktiveVarslerFromEventHandler = listOf(oppgave))
        mockVarselApi(
            varselConsumer = setupVarselConsumer(),
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )

        val aktiveVarsler: AktiveVarsler = client.get("/aktive").bodyFromJson()
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

        val inaktiveVarsler: List<InaktivtVarsel> = client.get("/inaktive").bodyFromJson()
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


    @Test
    fun `er bakoverkompatibel med frontend ved inaktivering av beskjed`() = testApplication {
        val expeectedEventId = "hhuu33-91sdf-shdkfh"
        var postCount = 0
        externalServices {
            hosts(varselAuthorityTestUrl) {
                install(ContentNegotiation) { jackson() }
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
            authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
        )
        client.post("/beskjed/inaktiver") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"eventId": "$expeectedEventId"}""")
        }.status shouldBe HttpStatusCode.OK

        postCount shouldBe 1
    }

    @Test
    fun `bruker preferert spraak i kall til authority`() {
        testApplication {
            setupVarselAuthority(expectedSpraakkodeParam = "en")
            mockVarselApi(
                varselConsumer = setupVarselConsumer(),
                authMockInstaller = installAuthenticatedMock(LevelOfAssurance.LEVEL_4)
            )

            client.get("/aktive?preferert_spraak=en").apply {
                status shouldBe HttpStatusCode.OK
            }

            client.get("/inaktive?preferert_spraak=en").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    private fun varselRoutesTest(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                jackson { jsonConfig() }
            }
        }.let { block(it) }
    }

    private suspend inline fun <reified T> HttpResponse.bodyFromJson(): T = objectMapper.readValue(bodyAsText())
}
