package no.nav.tms.varsel.api

import no.nav.tms.varsel.api.varsel.Varsel
import no.nav.tms.varsel.api.varsel.VarselType
import java.time.ZoneOffset
import java.time.ZonedDateTime

object VarselTestData {
    fun varsel(
        type: VarselType = VarselType.BESKJED,
        forstBehandlet: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        eventId: String = "12345",
        tekst: String = "tekst",
        link: String = "http://link.no",
        isMasked: Boolean = false,
        sistOppdatert: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        sikkerhetsnivaa: Int = 4,
        aktiv: Boolean = true,
        eksternVarsling: Boolean = false,
        prefererteKanaler: List<String> = emptyList(),
        fristUtløpt: Boolean? = false
    ) = Varsel(
        type = type,
        eventId = eventId,
        forstBehandlet = forstBehandlet,
        tekst = tekst,
        link = link,
        isMasked = isMasked,
        sikkerhetsnivaa = sikkerhetsnivaa,
        sistOppdatert = sistOppdatert,
        aktiv = aktiv,
        eksternVarslingSendt = eksternVarsling,
        eksternVarslingKanaler = prefererteKanaler,
        fristUtløpt = fristUtløpt
    )
}
