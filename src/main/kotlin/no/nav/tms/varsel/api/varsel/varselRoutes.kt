package no.nav.tms.varsel.api.varsel

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory


fun Route.varsel(
    varselConsumer: VarselConsumer
) {
    get("inaktive") {
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(call.userToken)

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }

    get("aktive") {
        val aktiveVarsler = varselConsumer.getAktiveVarsler(call.userToken)

        call.respond(HttpStatusCode.OK, aktiveVarsler)
    }

    get("antall/aktive") {
        val antallAktive = varselConsumer.getAktiveVarsler(call.userToken).let {
            AntallVarsler(
                beskjeder = it.beskjeder.size,
                oppgaver = it.oppgaver.size,
                innbokser = it.innbokser.size
            )
        }

        call.respond(HttpStatusCode.OK, antallAktive)
    }

    post("beskjed/inaktiver") {
        varselConsumer.postInaktiver(varselId = call.varselId(), userToken = call.userToken)
        call.respond(HttpStatusCode.OK)
    }
}

private val ApplicationCall.userToken get() = IdportenUserFactory.createIdportenUser(this).tokenString

@Serializable
data class AntallVarsler(val beskjeder: Int, val oppgaver: Int, val innbokser: Int)

@Serializable
data class InaktiverVarselBody(
    val eventId: String? = null,
    val varselId: String? = null
)

private suspend fun ApplicationCall.varselId(): String = receive<InaktiverVarselBody>().let {
    it.varselId ?: it.eventId ?: throw IllegalArgumentException("Mangler varselId eller eventId i body")
}
