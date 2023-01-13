package no.nav.tms.varsel.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.varsel.api.config.Environment
import no.nav.tms.varsel.api.config.jsonConfig

fun main() {
    val environment = Environment()

    val httpClient = HttpClient(Apache.create()) {
        install(ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)
    }

    embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment {
            rootPath = "tms-varsel-api"
            module {
                varselApi(
                    corsAllowedOrigins = environment.corsAllowedOrigins,
                    corsAllowedSchemes = environment.corsAllowedSchemes,
                    corsAllowedHeaders = environment.corsAllowedHeaders,
                    httpClient = httpClient,
                    varselConsumer = VarselConsumer(
                        client = httpClient,
                        eventHandlerBaseURL = environment.eventHandlerURL,
                        eventhandlerClientId = environment.eventhandlerClientId,
                        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
                    )
                )
            }
            connector {
                port = 8080
            }
        }
    ).start(wait = true)
}