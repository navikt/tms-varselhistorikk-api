package no.nav.tms.varsel.api.varsel

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUser
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory

fun Route.varselbjelle(varselConsumer: VarselConsumer) {
    route("/varselbjelle") {
        get("/varsler") {
            varselConsumer.getVarselbjelleVarsler(
                userToken = idportenUser.tokenString,
                preferertSpraak = call.request.preferertSpraak
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

fun Route.bjellevarsler(varselConsumer: VarselConsumer) {
    get("/bjellevarsler") {
        varselConsumer.getVarselbjelleVarsler(
            userToken = tokenxUser.tokenString,
            preferertSpraak = call.request.preferertSpraak
        ).let {
            call.respond(HttpStatusCode.OK, it)
        }
    }
}

private val ApplicationRequest.preferertSpraak get() = queryParameters["preferert_spraak"]?.lowercase()

private val PipelineContext<Unit, ApplicationCall>.idportenUser: IdportenUser
    get() = IdportenUserFactory.createIdportenUser(this.call)

private val PipelineContext<Unit, ApplicationCall>.tokenxUser: TokenXUser
    get() = TokenXUserFactory.createTokenXUser(this.call)
