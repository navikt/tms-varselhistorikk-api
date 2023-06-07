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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


fun Route.varsel(
    varselConsumer: VarselConsumer,
    tokenResolver: PipelineContext<Unit, ApplicationCall>.() -> String
) {
    get("inaktive") {
        val inaktiveVarsler = varselConsumer.getInaktiveVarsler(tokenResolver())

        call.respond(HttpStatusCode.OK, inaktiveVarsler)
    }

    get("aktive") {
        val aktiveVarsler = varselConsumer.getAktiveVarsler(tokenResolver())

        call.respond(HttpStatusCode.OK, aktiveVarsler)
    }

    get("antall/aktive") {
        val antallAktive = varselConsumer.getAktiveVarsler(tokenResolver()).let {
            AntallVarsler(
                beskjeder = it.beskjeder.size,
                oppgaver = it.oppgaver.size,
                innbokser = it.innbokser.size
            )
        }

        call.respond(HttpStatusCode.OK, antallAktive)
    }

    post("beskjed/inaktiver") {
        varselConsumer.postInaktiver(eventId = call.eventId(), userToken = tokenResolver())
        call.respond(HttpStatusCode.OK)
    }
}

@Serializable
data class AntallVarsler(val beskjeder: Int, val oppgaver: Int, val innbokser: Int)


private suspend fun ApplicationCall.eventId(): String = receive<String>().let {
    if (it.isEmpty()) {
        throw IllegalArgumentException("request mangler body innhold")
    }
    Json.parseToJsonElement(it).jsonObject["eventId"]?.jsonPrimitive?.content
        ?: throw IllegalArgumentException("eventId finnes ikke i body")
}
