package no.nav.tms.varsel.api

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.netty.handler.codec.http.HttpObjectDecoder
import io.netty.handler.codec.http.HttpServerCodec
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
                    httpClient = httpClient,
                    varselConsumer = VarselConsumer(
                        client = httpClient,
                        varselAuthorityUrl = "http://tms-varsel-authority",
                        varselAuthorityClientId = environment.eventhandlerClientId,
                        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
                    )
                )
            }
            connector {
                port = 8080
            }
        }
    ) {
        httpServerCodec = {
            HttpServerCodec(
                HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH,
                16 * 1024, // max header size
                HttpObjectDecoder.DEFAULT_MAX_CHUNK_SIZE
            )
        }
    }.start(wait = true)
}
