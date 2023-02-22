@file:UseSerializers(ZonedDateTimeSerializer::class)

package no.nav.tms.varsel.api.varsel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.tms.varsel.api.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class AktivtVarsel(
    val eventId: String,
    val forstBehandlet: ZonedDateTime,
    val isMasked: Boolean,
    val tekst: String?,
    val link: String?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = AktivtVarsel(
            eventId = varsel.eventId,
            forstBehandlet = varsel.forstBehandlet,
            isMasked = varsel.isMasked,
            tekst = varsel.tekst,
            link = varsel.link,
            eksternVarslingSendt = varsel.eksternVarslingSendt,
            eksternVarslingKanaler = varsel.eksternVarslingKanaler
        )
    }
}

@Serializable
data class AktiveVarsler(
    val beskjeder: List<AktivtVarsel>,
    val oppgaver: List<AktivtVarsel>,
    val innbokser: List<AktivtVarsel>
) {
    companion object {
        fun fromVarsler(varsler: List<Varsel>) = AktiveVarsler(
            beskjeder = varsler.filter { it.type == VarselType.BESKJED }.map { AktivtVarsel.fromVarsel(it) },
            oppgaver = varsler.filter { it.type == VarselType.OPPGAVE }.map { AktivtVarsel.fromVarsel(it) },
            innbokser = varsler.filter { it.type == VarselType.INNBOKS }.map { AktivtVarsel.fromVarsel(it) }
        )
    }
}