package no.nav.tms.varsel.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

fun Route.varsel(
    varselConsumer: VarselConsumer
) {
    get("inaktive") {
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(accessToken, loginLevel)

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }
}

private val PipelineContext<Unit, ApplicationCall>.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

private val PipelineContext<Unit, ApplicationCall>.loginLevel
    get() = IdportenUserFactory.createIdportenUser(call).loginLevel
