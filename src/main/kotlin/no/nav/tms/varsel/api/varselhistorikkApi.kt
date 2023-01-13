package no.nav.tms.varsel.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tms.token.support.idporten.sidecar.LoginLevel
import no.nav.tms.token.support.idporten.sidecar.installIdPortenAuth
import no.nav.tms.varsel.api.config.jsonConfig


fun Application.varselApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    httpClient: HttpClient,
    varselConsumer: VarselConsumer,
    authInstaller: Application.() -> Unit = {
        installIdPortenAuth {
            setAsDefault = true
            loginLevel = LoginLevel.LEVEL_3
        }
    }
) {
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(DefaultHeaders)

    authInstaller()

    install(StatusPages) {

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
        health(collectorRegistry)

        authenticate {
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

suspend inline fun <reified T> HttpClient.get(url: String, accessToken: String): T = withContext(Dispatchers.IO) {
    request {
        url(url)
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }.body()
}