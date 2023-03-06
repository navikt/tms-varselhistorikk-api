@file:UseSerializers(ZonedDateTimeSerializer::class)

package no.nav.tms.varsel.api.varsel

import io.ktor.client.HttpClient
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
)

enum class VarselType {
    OPPGAVE,
    BESKJED,
    INNBOKS,
}