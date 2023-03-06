@file:UseSerializers(ZonedDateTimeSerializer::class)

package no.nav.tms.varsel.api.varsel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.tms.varsel.api.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class InaktivtVarsel(
    val eventId: String,
    val forstBehandlet: ZonedDateTime,
    val type: VarselType,
    val isMasked: Boolean,
    val tekst: String?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = InaktivtVarsel(
            eventId = varsel.eventId,
            forstBehandlet = varsel.forstBehandlet,
            type = varsel.type,
            isMasked = varsel.isMasked,
            tekst = varsel.tekst,
            eksternVarslingSendt = varsel.eksternVarslingSendt,
            eksternVarslingKanaler = varsel.eksternVarslingKanaler
        )
    }
}
