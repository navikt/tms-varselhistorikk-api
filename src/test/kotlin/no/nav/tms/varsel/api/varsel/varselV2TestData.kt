package no.nav.tms.varsel.api.varsel

import java.time.ZonedDateTime

object VarselV2TestData {
    fun incomingVarsel(
        type: VarselType = VarselType.beskjed,
        varselId: String = "123143",
        aktiv: Boolean = true,
        innhold: VarselAuthority.Innhold? = VarselAuthority.Innhold(
            link = "www.nav.no/test",
            spraakkode = "nb",
            tekst = "Varsel test tekst"
        ),
        eksternVarslingSendt: Boolean = false,
        eksternVarslingKanaler: List<String> = listOf("SMS", "EPOST"),
        opprettet: ZonedDateTime = ZonedDateTime.now(),
        aktivFremTil: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        inaktivert: ZonedDateTime? = null
    ): VarselAuthority.Varsel {
        return VarselAuthority.Varsel(
            type = type,
            varselId = varselId,
            aktiv = aktiv,
            innhold = innhold,
            eksternVarslingSendt = eksternVarslingSendt,
            eksternVarslingKanaler = eksternVarslingKanaler,
            opprettet = opprettet,
            aktivFremTil = aktivFremTil,
            inaktivert = inaktivert
        )
    }
}
