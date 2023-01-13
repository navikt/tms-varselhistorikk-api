package no.nav.tms.varsel.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.varsel(
    varselConsumer: VarselConsumer
) {
    get("inaktive") {
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(accessToken, loginLevel)

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }
}
