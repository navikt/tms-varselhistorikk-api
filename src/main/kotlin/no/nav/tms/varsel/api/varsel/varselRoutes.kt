package no.nav.tms.varsel.api.varsel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tms.varsel.api.accessToken
import no.nav.tms.varsel.api.loginLevel

fun Route.varsel(
    varselConsumer: VarselConsumer
) {
    get("inaktive") {
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(accessToken, loginLevel)

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }

    get("aktive") {
        val aktiveVarsler = varselConsumer.getAktiveVarsler(accessToken)

        call.respond(HttpStatusCode.OK, aktiveVarsler)
    }
}
