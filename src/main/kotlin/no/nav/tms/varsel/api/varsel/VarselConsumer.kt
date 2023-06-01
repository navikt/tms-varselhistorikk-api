@file:UseSerializers(ZonedDateTimeSerializer::class)

package no.nav.tms.varsel.api.varsel

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.varsel.api.ZonedDateTimeSerializer
import no.nav.tms.varsel.api.get
import java.time.ZonedDateTime

class VarselConsumer(
    private val client: HttpClient,
    private val eventHandlerBaseURL: String,
    private val eventhandlerClientId: String,
    private val eventAggregatorBaseURL: String,
    private val eventAggregaorClientId: String,
    private val tokendingsService: TokendingsService,
) {
    suspend fun getAktiveVarsler(userToken: String): AktiveVarsler {
        val eventhandlerToken = tokendingsService.exchangeToken(userToken, targetApp = eventhandlerClientId)
        val varsler: List<Varsel> = client.get("$eventHandlerBaseURL/fetch/varsel/aktive", eventhandlerToken)

        return AktiveVarsler.fromVarsler(varsler)
    }

    suspend fun getInaktiveVarsler(userToken: String): List<InaktivtVarsel> {
        val eventhandlerToken = tokendingsService.exchangeToken(userToken, targetApp = eventhandlerClientId)
        val varsler: List<Varsel> = client.get("$eventHandlerBaseURL/fetch/varsel/inaktive", eventhandlerToken)

        return varsler.map { InaktivtVarsel.fromVarsel(it) }
    }

    suspend fun getVarselbjelleVarsler(userToken: String, authLevel: Int): VarselbjelleVarsler {
        val eventhandlerToken = tokendingsService.exchangeToken(userToken, targetApp = eventhandlerClientId)
        val varsler: List<Varsel> = client.get("$eventHandlerBaseURL/fetch/varsel/aktive", eventhandlerToken)

        return VarselbjelleVarsler.fromVarsler(varsler, authLevel)
    }

    suspend fun postInaktiver(userToken: String, eventId: String) {
        val aggregatorToken = tokendingsService.exchangeToken(userToken, eventAggregaorClientId)
        client.post("$eventAggregatorBaseURL/beskjed/done") {
            header(HttpHeaders.Authorization, "Bearer $aggregatorToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"eventId": "$eventId"}""")
        }
    }
}

@Serializable
data class Varsel(
    val eventId: String,
    val sikkerhetsnivaa: Int,
    val sistOppdatert: ZonedDateTime,
    val tekst: String?,
    val link: String?,
    val isMasked: Boolean,
    val aktiv: Boolean,
    val type: VarselType,
    val forstBehandlet: ZonedDateTime,
    val fristUtløpt: Boolean?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    fun toVarselbjelleVarsel(authLevel: Int) = VarselbjelleVarsel(
        eventId = eventId,
        tidspunkt = forstBehandlet,
        isMasked = sikkerhetsnivaa > authLevel,
        tekst = if (sikkerhetsnivaa > authLevel) null else tekst,
        link = if (sikkerhetsnivaa > authLevel) null else link,
        type = type.name,
        eksternVarslingSendt = eksternVarslingSendt,
        eksternVarslingKanaler = eksternVarslingKanaler
    )
}

enum class VarselType {
    OPPGAVE,
    BESKJED,
    INNBOKS,
}