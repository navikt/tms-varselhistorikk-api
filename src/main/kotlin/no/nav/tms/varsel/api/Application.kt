package no.nav.tms.varsel.api

import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import no.nav.tms.varsel.api.varsel.VarselConsumer

fun main() {
    val environment = Environment()

    val httpClient = HttpClientBuilder.build()

    embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment {
            rootPath = "tms-varsel-api"
            module {
                varselApi(
                    corsAllowedOrigins = environment.corsAllowedOrigins,
                    corsAllowedSchemes = environment.corsAllowedSchemes,
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