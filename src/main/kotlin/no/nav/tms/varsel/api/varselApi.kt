package no.nav.tms.varsel.api

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import no.nav.tms.token.support.tokenx.validation.installTokenXAuth
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.varsel

const val SIDECAR_WORKAROUND_HEADER = "PROXY_TOKEN"


fun Application.varselApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    httpClient: HttpClient,
    varselConsumer: VarselConsumer,
    authInstaller: Application.() -> Unit = {
        installTokenXAuth {
            setAsDefault = true
        }
    }
) {
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val securelog = KotlinLogging.logger("secureLog")

    install(DefaultHeaders)
    install(RouteByAuthenticationMethod)

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
        allowHost(corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(jsonConfig())
    }

    install(MicrometerMetrics) {
        registry = collectorRegistry
    }

    routing {
        meta(collectorRegistry)
        authenticate {
            varsel(varselConsumer)
        }

        route("/tokenx"){
            varsel(varselConsumer)
        }
    }

    configureShutdownHook(httpClient)
}

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

val PipelineContext<Unit, ApplicationCall>.userToken: String
    get() = TokenXUserFactory.createTokenXUser(call).tokenString

fun jsonConfig(): Json {
    return Json {
        this.ignoreUnknownKeys = true
        this.encodeDefaults = true
    }
}

val RouteByAuthenticationMethod = createApplicationPlugin(name = "RouteByAuthenticationMethod") {
    on(CallSetup) { call ->
        if (call.request.headers.contains(SIDECAR_WORKAROUND_HEADER)){
            val originalUri = call.request.uri
            call.mutableOriginConnectionPoint.uri = "/tokenx/$originalUri"
        }
        println("Pipeline gogo")
    }
}