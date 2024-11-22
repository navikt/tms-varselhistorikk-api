package no.nav.tms.varsel.api.varsel.v2

import no.nav.tms.varsel.api.varsel.VarselAuthority
import no.nav.tms.varsel.api.varsel.VarselType

data class AlleVarsler(
    val hasMaskedVarsel: Boolean,
    val aktive: AktiveVarslerV2,
    val inaktive: List<Varsel>,
) {
    companion object {
        fun fromVarsler(varsler: List<VarselAuthority.Varsel>): AlleVarsler {
            val aktivBeskjeder = mutableListOf<Varsel>()
            val aktivOppgaver = mutableListOf<Varsel>()
            val inaktivtVarseler = mutableListOf<Varsel>()
            var hasMaskedVarsel = false

            varsler.map {
                if(it.innhold == null) {
                    hasMaskedVarsel = true
                }

                if (it.aktiv) {
                    if (it.type == VarselType.oppgave) {
                        aktivOppgaver.add(Varsel.fromVarsel(it))
                    } else {
                        aktivBeskjeder.add(Varsel.fromVarsel(it))
                    }
                } else {
                    inaktivtVarseler.add(Varsel.fromVarsel(it))
                }
            }
            return AlleVarsler(
                hasMaskedVarsel = hasMaskedVarsel,
                aktive = AktiveVarslerV2(aktivBeskjeder, aktivOppgaver),
                inaktive = inaktivtVarseler
            )
        }
    }
}