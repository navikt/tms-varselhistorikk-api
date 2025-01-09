package no.nav.tms.varsel.api.varsel.v2

import no.nav.tms.varsel.api.varsel.VarselAuthority
import no.nav.tms.varsel.api.varsel.VarselType
import java.time.ZonedDateTime

data class Varsel(
    @Deprecated("Use varselId") val eventId: String,
    @Deprecated("Use tidspunkt") val forstBehandlet: ZonedDateTime,
    val isMasked: Boolean,
    val spraakkode: String?,
    val tekst: String?,
    val link: String?,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>,
    val type: VarselType,
    val isInaktiverbar: Boolean,

    ) {
    companion object {
        fun fromVarsel(varsel: VarselAuthority.Varsel): Varsel {
            val isMasked = varsel.innhold == null
            val isInaktivertOppgave = varsel.type == VarselType.oppgave && !varsel.aktiv

            return Varsel(
                eventId = varsel.varselId,
                forstBehandlet = varsel.opprettet,
                isMasked = isMasked,
                spraakkode = varsel.innhold?.spraakkode,
                tekst = varsel.innhold?.tekst,
                link = if (isInaktivertOppgave) null else varsel.innhold?.link,
                eksternVarslingSendt = varsel.eksternVarslingSendt,
                eksternVarslingKanaler = varsel.eksternVarslingKanaler,
                type = if (varsel.type == VarselType.oppgave) VarselType.oppgave else VarselType.beskjed,
                isInaktiverbar = varsel.type == VarselType.beskjed && varsel.aktiv && !isMasked,
            )
        }
    }
}