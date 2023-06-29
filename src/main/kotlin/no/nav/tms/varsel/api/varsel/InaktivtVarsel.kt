@file:UseSerializers(ZonedDateTimeSerializer::class)

package no.nav.tms.varsel.api.varsel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.tms.varsel.api.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class InaktivtVarsel(
    val type: VarselType,
    @Deprecated("Use varselId") val eventId: String,
    val varselId: String,
    @Deprecated("Use tidspunkt") val forstBehandlet: ZonedDateTime,
    val tidspunkt: ZonedDateTime,
    val isMasked: Boolean,
    val tekst: String?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = InaktivtVarsel(
            type = varsel.type,
            eventId = varsel.varselId,
            varselId = varsel.varselId,
            forstBehandlet = varsel.opprettet,
            tidspunkt = varsel.opprettet,
            isMasked = varsel.innhold == null,
            tekst = varsel.innhold?.tekst,
            eksternVarslingSendt = varsel.eksternVarslingSendt,
            eksternVarslingKanaler = varsel.eksternVarslingKanaler
        )
    }
}
