package no.nav.tms.varsel.api.varsel

import java.time.ZonedDateTime

data class InaktivtVarsel(
    val type: VarselType,
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
        fun fromVarsel(varsel: VarselAuthority.Varsel) = InaktivtVarsel(
            type = varsel.type,
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
