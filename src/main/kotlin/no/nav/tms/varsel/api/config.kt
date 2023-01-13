package no.nav.tms.varsel.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val corsAllowedOrigins: String = getEnvVar("CORS_ALLOWED_ORIGINS"),
    val corsAllowedSchemes: String = getEnvVar("CORS_ALLOWED_SCHEMES","https"),
    val eventHandlerURL: String = getEnvVar("EVENT_HANDLER_URL"),
    val eventhandlerClientId: String = getEnvVar("EVENTHANDLER_CLIENT_ID"),
)

object HttpClientBuilder {

    fun build(httpClientEngine: HttpClientEngine = Apache.create()): HttpClient {
        return HttpClient(httpClientEngine) {
            install(ContentNegotiation) {
                json(jsonConfig())
            }
            install(HttpTimeout)
        }
    }
}

suspend inline fun <reified T> HttpClient.get(url: String, accessToken: String): T = withContext(Dispatchers.IO) {
    request {
        url(url)
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }.body()
}