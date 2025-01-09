package no.nav.tms.varsel.api.varsel.v2

import no.nav.tms.varsel.api.varsel.VarselAuthority
import no.nav.tms.varsel.api.varsel.VarselType


data class AktiveVarslerV2(val beskjeder: List<Varsel>, val oppgaver: List<Varsel>) {
    companion object {
        fun fromVarsler(varsler: List<VarselAuthority.Varsel>): AktiveVarslerV2 {
            val beskjeder = mutableListOf<Varsel>()
            val oppgaver = mutableListOf<Varsel>()

            varsler.map {
                if (it.type == VarselType.oppgave) {
                    oppgaver.add(Varsel.fromVarsel(it))
                } else {
                    beskjeder.add(Varsel.fromVarsel(it))
                }
            }
            return AktiveVarslerV2(beskjeder = beskjeder, oppgaver = oppgaver)
        }
    }
}