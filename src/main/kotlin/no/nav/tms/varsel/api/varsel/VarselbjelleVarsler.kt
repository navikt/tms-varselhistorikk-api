package no.nav.tms.varsel.api.varsel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.ZonedDateTime

data class VarselbjelleVarsler(
    val beskjeder: List<VarselbjelleVarsel>,
    val oppgaver: List<VarselbjelleVarsel>,
) {
    companion object {
        fun fromVarsler(varsler: List<VarselAuthority.Varsel>): VarselbjelleVarsler {
            val groupedVarsler = varsler.groupBy { it.type }.mapValues { (_, varsler) ->
                varsler.map(VarselbjelleVarsel::fromVarsel)
            }

            return VarselbjelleVarsler(
                beskjeder = (groupedVarsler[VarselType.beskjed] ?: emptyList()) + (groupedVarsler[VarselType.innboks]
                    ?: emptyList()),
                oppgaver = groupedVarsler[VarselType.oppgave] ?: emptyList()
            )
        }
    }
}

data class VarselbjelleVarsel(
    val eventId: String,
    val varselId: String,
    val tidspunkt: ZonedDateTime,
    val isMasked: Boolean,
    val spraakkode: String?,
    val tekst: String?,
    val link: String?,
    val type: String,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    companion object {
        fun fromVarsel(varsel: VarselAuthority.Varsel) = with(varsel) {
            VarselbjelleVarsel(
                eventId = varselId,
                varselId = varselId,
                tidspunkt = opprettet,
                isMasked = innhold == null,
                spraakkode = innhold?.spraakkode,
                tekst = innhold?.tekst,
                link = innhold?.link,
                type = type.name,
                eksternVarslingSendt = eksternVarslingSendt,
                eksternVarslingKanaler = eksternVarslingKanaler
            )
        }
    }
}

