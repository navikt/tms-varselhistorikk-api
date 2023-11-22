package no.nav.tms.varsel.api.varsel

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
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
            call.respond(varselConsumer.getVarselbjelleVarsler(idportenUser.tokenString))
        }
    }
}

fun Route.bjellevarsler(varselConsumer: VarselConsumer) {
    get("/bjellevarsler") {
        call.respond(varselConsumer.getVarselbjelleVarsler(tokenxUser.tokenString))
    }
}


private val PipelineContext<Unit, ApplicationCall>.idportenUser: IdportenUser
    get() = IdportenUserFactory.createIdportenUser(this.call)

private val PipelineContext<Unit, ApplicationCall>.tokenxUser: TokenXUser
    get() = TokenXUserFactory.createTokenXUser(this.call)
