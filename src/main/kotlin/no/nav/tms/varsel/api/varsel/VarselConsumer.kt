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
    suspend fun getInaktiveVarsler(userToken: String, loginLevel: Int): List<InaktivtVarsel> {
        val eventhandlerToken = tokendingsService.exchangeToken(userToken, targetApp = eventhandlerClientId)

        return getInaktiveBeskjeder(eventhandlerToken).map { InaktivtVarsel.fromBeskjed(it, loginLevel) } +
                getInaktiveOppgaver(eventhandlerToken).map { InaktivtVarsel.fromOppgave(it, loginLevel) } +
                getInaktiveInnbokser(eventhandlerToken).map { InaktivtVarsel.fromInnboks(it, loginLevel) }
    }

    suspend fun getAktiveVarsler(userToken: String): AktiveVarsler {
        val eventhandlerToken = tokendingsService.exchangeToken(userToken, targetApp = eventhandlerClientId)
        val varsler: List<Varsel> = client.get("$eventHandlerBaseURL/fetch/varsel/aktive", eventhandlerToken)

        return AktiveVarsler.fromVarsler(varsler)
    }

    private suspend fun getInaktiveBeskjeder(eventhandlerToken: String): List<Beskjed> =
        client.get("$eventHandlerBaseURL/fetch/beskjed/inaktive", eventhandlerToken)

    private suspend fun getInaktiveOppgaver(eventhandlerToken: String): List<Oppgave> =
        client.get("$eventHandlerBaseURL/fetch/oppgave/inaktive", eventhandlerToken)

    private suspend fun getInaktiveInnbokser(eventhandlerToken: String): List<Innboks> =
        client.get("$eventHandlerBaseURL/fetch/innboks/inaktive", eventhandlerToken)
}

@Serializable
data class Beskjed(
    val fodselsnummer: String,
    val eventId: String,
    val eventTidspunkt: ZonedDateTime,
    val forstBehandlet: ZonedDateTime,
    val sikkerhetsnivaa: Int,
    val sistOppdatert: ZonedDateTime,
    val synligFremTil: ZonedDateTime?,
    val tekst: String,
    val link: String,
    val aktiv: Boolean,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
)

@Serializable
data class Oppgave(
    val fodselsnummer: String,
    val eventId: String,
    val forstBehandlet: ZonedDateTime,
    val sikkerhetsnivaa: Int,
    val sistOppdatert: ZonedDateTime,
    val tekst: String,
    val link: String,
    val aktiv: Boolean,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
)

@Serializable
data class Innboks(
    val forstBehandlet: ZonedDateTime,
    val fodselsnummer: String,
    val eventId: String,
    val tekst: String,
    val link: String,
    val sikkerhetsnivaa: Int,
    val sistOppdatert: ZonedDateTime,
    val aktiv: Boolean,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
)

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