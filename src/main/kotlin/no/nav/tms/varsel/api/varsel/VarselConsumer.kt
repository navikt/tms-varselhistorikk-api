package no.nav.tms.varsel.api.varsel

import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.varsel.api.varsel.v2.AktiveVarslerV2
import no.nav.tms.varsel.api.varsel.v2.AlleVarsler
import java.time.ZonedDateTime

class VarselConsumer(
    private val client: HttpClient,
    private val varselAuthorityUrl: String,
    private val varselAuthorityClientId: String,
    private val tokendingsService: TokendingsService
) {
    suspend fun getAktiveVarsler(userToken: String, preferertSpraak: String?): AktiveVarsler {
        return getVarsler(userToken, "/varsel/sammendrag/aktive", preferertSpraak = preferertSpraak)
            .let (AktiveVarsler::fromVarsler)
    }

    suspend fun getInaktiveVarsler(userToken: String, preferertSpraak: String?): List<InaktivtVarsel> {
        return getVarsler(userToken,"/varsel/sammendrag/inaktive", preferertSpraak = preferertSpraak)
            .map(InaktivtVarsel::fromVarsel)
    }

    suspend fun getVarselbjelleVarsler(userToken: String, preferertSpraak: String?): VarselbjelleVarsler {
        return getVarsler(userToken, "/varsel/sammendrag/aktive", preferertSpraak = preferertSpraak)
            .let(VarselbjelleVarsler::fromVarsler)
    }

    suspend fun getAlleVarsler(userToken: String, preferertSpraak: String?): AlleVarsler {
        return getVarsler(userToken, "/varsel/sammendrag/alle", preferertSpraak = preferertSpraak)
            .let(AlleVarsler::fromVarsler)
    }

    suspend fun getAktiveVarslerV2(userToken: String, preferertSpraak: String?): AktiveVarslerV2 {
        return getVarsler(userToken, "/varsel/sammendrag/aktive", preferertSpraak = preferertSpraak)
            .let (AktiveVarslerV2::fromVarsler)
    }

    suspend fun postInaktiver(userToken: String, varselId: String) {
        val authorityToken = tokendingsService.exchangeToken(userToken, varselAuthorityClientId)

        client.post("$varselAuthorityUrl/beskjed/inaktiver") {
            header(HttpHeaders.Authorization, "Bearer $authorityToken")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"varselId": "$varselId"}""")
        }
    }

    private suspend fun getVarsler(userToken: String, path: String, preferertSpraak: String? = null): List<VarselAuthority.Varsel> {
        val authorityToken = tokendingsService.exchangeToken(userToken, targetApp = varselAuthorityClientId)

        return client.request {
            url("$varselAuthorityUrl$path")
            method = HttpMethod.Get
            header(HttpHeaders.Authorization, "Bearer $authorityToken")

            preferertSpraak?.let { parameter("preferert_spraak", it) }
        }.body()
    }
}

object VarselAuthority {
    data class Varsel(
        val type: VarselType,
        val varselId: String,
        val aktiv: Boolean,
        val innhold: Innhold?,
        val eksternVarslingSendt: Boolean,
        val eksternVarslingKanaler: List<String>,
        val opprettet: ZonedDateTime,
        val aktivFremTil: ZonedDateTime?,
        val inaktivert: ZonedDateTime?
    )

    data class Innhold(
        val spraakkode: String,
        val tekst: String,
        val link: String?
    )
}




enum class VarselType {
    oppgave,
    beskjed,
    innboks,
}
