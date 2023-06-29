@file:UseSerializers(ZonedDateTimeSerializer::class)

package no.nav.tms.varsel.api.varsel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.tms.varsel.api.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class AktivtVarsel(
    @Deprecated("Use varselId") val eventId: String,
    val varselId: String,
    @Deprecated("Use tidspunkt") val forstBehandlet: ZonedDateTime,
    val tidspunkt: ZonedDateTime,
    val isMasked: Boolean,
    val tekst: String?,
    val link: String?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = AktivtVarsel(
            eventId = varsel.varselId,
            varselId = varsel.varselId,
            forstBehandlet = varsel.opprettet,
            tidspunkt = varsel.opprettet,
            isMasked = varsel.innhold == null,
            tekst = varsel.innhold?.tekst,
            link = varsel.innhold?.link,
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
            beskjeder = varsler.filter { it.type == VarselType.beskjed }.map { AktivtVarsel.fromVarsel(it) },
            oppgaver = varsler.filter { it.type == VarselType.oppgave }.map { AktivtVarsel.fromVarsel(it) },
            innbokser = varsler.filter { it.type == VarselType.innboks }.map { AktivtVarsel.fromVarsel(it) }
        )
    }
}
