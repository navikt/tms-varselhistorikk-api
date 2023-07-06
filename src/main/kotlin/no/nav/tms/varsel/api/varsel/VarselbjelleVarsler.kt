@file:UseSerializers(ZonedDateTimeSerializer::class)
package no.nav.tms.varsel.api.varsel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.tms.varsel.api.ZonedDateTimeSerializer
import java.time.ZonedDateTime


@Serializable
data class VarselbjelleVarsler(
    val beskjeder: List<VarselbjelleVarsel>,
    val oppgaver: List<VarselbjelleVarsel>,
) {
    companion object {
        fun fromVarsler(varsler: List<Varsel>): VarselbjelleVarsler {
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

@Serializable
data class VarselbjelleVarsel(
    val eventId: String,
    val varselId: String,
    val tidspunkt: ZonedDateTime,
    val isMasked: Boolean,
    val tekst: String?,
    val link: String?,
    val type: String,
    val eksternVarslingSendt: Boolean,
    val eksternVarslingKanaler: List<String>
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = with(varsel) {
            VarselbjelleVarsel(
                eventId = varselId,
                varselId = varselId,
                tidspunkt = opprettet,
                isMasked = innhold == null,
                tekst = innhold?.tekst,
                link = innhold?.link,
                type = type.name,
                eksternVarslingSendt = eksternVarslingSendt,
                eksternVarslingKanaler = eksternVarslingKanaler
            )
        }
    }
}

