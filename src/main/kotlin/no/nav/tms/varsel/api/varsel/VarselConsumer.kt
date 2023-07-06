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
    private val varselAuthorityUrl: String,
    private val varselAuthorityClientId: String,
    private val tokendingsService: TokendingsService
) {
    suspend fun getAktiveVarsler(userToken: String): AktiveVarsler {
        return getVarsler(userToken, "/varsel/sammendrag/aktive")
            .let (AktiveVarsler::fromVarsler)
    }

    suspend fun getInaktiveVarsler(userToken: String): List<InaktivtVarsel> {
        return getVarsler( userToken,"/varsel/sammendrag/inaktive")
            .map(InaktivtVarsel::fromVarsel)
    }

    suspend fun getVarselbjelleVarsler(userToken: String): VarselbjelleVarsler {
        return getVarsler(userToken, "/varsel/sammendrag/aktive")
            .let(VarselbjelleVarsler::fromVarsler)
    }

    suspend fun postInaktiver(userToken: String, varselId: String) {
        val authorityToken = tokendingsService.exchangeToken(userToken, varselAuthorityClientId)

        client.post("$varselAuthorityUrl/beskjed/inaktiver") {
            header(HttpHeaders.Authorization, "Bearer $authorityToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"varselId": "$varselId"}""")
        }
    }

    private suspend fun getVarsler(userToken: String, path: String): List<Varsel> {
        val authorityToken = tokendingsService.exchangeToken(userToken, targetApp = varselAuthorityClientId)
        return client.get("$varselAuthorityUrl$path", authorityToken)
    }
}

@Serializable
data class Varsel(
    val type: VarselType,
    val varselId: String,
    val aktiv: Boolean,
    val innhold: VarselInnhold?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>,
    val opprettet: ZonedDateTime,
    val aktivFremTil: ZonedDateTime?,
    val inaktivert: ZonedDateTime?
)

@Serializable
data class VarselInnhold(
    val tekst: String,
    val link: String?
)

enum class VarselType {
    oppgave,
    beskjed,
    innboks,
}
