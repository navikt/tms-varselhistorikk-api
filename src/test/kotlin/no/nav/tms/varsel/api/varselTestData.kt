package no.nav.tms.varsel.api

import no.nav.tms.varsel.api.varsel.Beskjed
import no.nav.tms.varsel.api.varsel.Innboks
import no.nav.tms.varsel.api.varsel.Oppgave
import java.time.ZoneOffset
import java.time.ZonedDateTime

object VarselTestData {
    fun beskjed(
        eventTidspunkt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        forstBehandlet: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        synligFremTil: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1),
        fodselsnummer: String = "123",
        eventId: String = "12345",
        tekst: String = "tekst",
        link: String = "http://link.no",
        sistOppdatert: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        sikkerhetsnivaa: Int = 4,
        aktiv: Boolean = true,
        eksternVarsling: Boolean = false,
        prefererteKanaler: List<String> = emptyList(),
    ) = Beskjed(
        eventId = eventId,
        eventTidspunkt = eventTidspunkt,
        forstBehandlet = forstBehandlet,
        fodselsnummer = fodselsnummer,
        tekst = tekst,
        link = link,
        sikkerhetsnivaa = sikkerhetsnivaa,
        sistOppdatert = sistOppdatert,
        synligFremTil = synligFremTil,
        aktiv = aktiv,
        eksternVarslingSendt = eksternVarsling,
        eksternVarslingKanaler = prefererteKanaler
    )

    fun oppgave(
        forstBehandlet: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        fodselsnummer: String = "123",
        eventId: String = "12345",
        tekst: String = "tekst",
        link: String = "http://link.no",
        sistOppdatert: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        sikkerhetsnivaa: Int = 4,
        aktiv: Boolean = true,
        eksternVarsling: Boolean = false,
        prefererteKanaler: List<String> = emptyList(),
    ) = Oppgave(
        eventId = eventId,
        forstBehandlet = forstBehandlet,
        fodselsnummer = fodselsnummer,
        tekst = tekst,
        link = link,
        sikkerhetsnivaa = sikkerhetsnivaa,
        sistOppdatert = sistOppdatert,
        aktiv = aktiv,
        eksternVarslingSendt = eksternVarsling,
        eksternVarslingKanaler = prefererteKanaler
    )

    fun innboks(
        forstBehandlet: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        fodselsnummer: String = "123",
        eventId: String = "12345",
        tekst: String = "tekst",
        link: String = "http://link.no",
        sistOppdatert: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
        sikkerhetsnivaa: Int = 4,
        aktiv: Boolean = true,
        eksternVarsling: Boolean = false,
        prefererteKanaler: List<String> = emptyList(),
    ) = Innboks(
        eventId = eventId,
        forstBehandlet = forstBehandlet,
        fodselsnummer = fodselsnummer,
        tekst = tekst,
        link = link,
        sikkerhetsnivaa = sikkerhetsnivaa,
        sistOppdatert = sistOppdatert,
        aktiv = aktiv,
        eksternVarslingSendt = eksternVarsling,
        eksternVarslingKanaler = prefererteKanaler
    )
}
