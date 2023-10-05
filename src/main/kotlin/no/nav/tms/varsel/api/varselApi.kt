package no.nav.tms.varsel.api

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging
import nav.no.tms.common.metrics.installTmsMicrometerMetrics
import no.nav.tms.token.support.idporten.sidecar.LevelOfAssurance
import no.nav.tms.token.support.idporten.sidecar.idPorten
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.varsel
import no.nav.tms.varsel.api.varsel.varselbjelle

fun Application.varselApi(
    corsAllowedOrigins: String,
    httpClient: HttpClient,
    varselConsumer: VarselConsumer,
    authInstaller: Application.() -> Unit = {
        authentication {
            idPorten {
                setAsDefault = true
                levelOfAssurance = LevelOfAssurance.SUBSTANTIAL
            }
        }
    }
) {
    val securelog = KotlinLogging.logger("secureLog")

    install(DefaultHeaders)

    authInstaller()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            securelog.warn { "Kall til ${call.request.uri} feilet: ${cause.message}" }
            securelog.warn { cause.stackTrace }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(CORS) {
        allowCredentials = true
        allowHost(corsAllowedOrigins, schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(jsonConfig())
    }

    installTmsMicrometerMetrics {
        setupMetricsRoute = true
        installMicrometerPlugin = true
    }

    routing {
        metaRoutes()
        authenticate {
            varsel(varselConsumer)
            varselbjelle(varselConsumer)
        }

    }

    configureShutdownHook(httpClient)
}

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

fun jsonConfig(): Json {
    return Json {
        this.ignoreUnknownKeys = true
        this.encodeDefaults = true
    }
}
