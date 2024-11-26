package no.nav.tms.varsel.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.*
import no.nav.tms.common.metrics.installTmsMicrometerMetrics
import no.nav.tms.token.support.idporten.sidecar.LevelOfAssurance
import no.nav.tms.token.support.idporten.sidecar.idPorten
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX
import no.nav.tms.varsel.api.varsel.VarselConsumer
import no.nav.tms.varsel.api.varsel.bjellevarsler
import no.nav.tms.varsel.api.varsel.varsel
import no.nav.tms.varsel.api.varsel.varselbjelle
import no.nav.tms.varsel.api.varsel.alleVarsler

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
            tokenX {
                setAsDefault = false
                levelOfAssurance = TokenXLoa.SUBSTANTIAL
            }
        }
    }
) {
    val securelog = KotlinLogging.logger("secureLog")
    val log = KotlinLogging.logger{}

    install(DefaultHeaders)
    authInstaller()

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            log.error { "400 bad request" }
            call.respond(
                HttpStatusCode.BadRequest
            )
        }
        exception<Throwable> { call, cause ->
            securelog.warn(cause) { "Kall til ${call.request.uri} feilet: ${cause.message}" }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(CORS) {
        allowCredentials = true
        allowHost(corsAllowedOrigins, schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        jackson { jsonConfig() }
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
        authenticate(TokenXAuthenticator.name) {
            bjellevarsler(varselConsumer)
            alleVarsler(varselConsumer)
        }

    }

    configureShutdownHook(httpClient)
}

typealias TokenXLoa = no.nav.tms.token.support.tokenx.validation.LevelOfAssurance

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

fun ObjectMapper.jsonConfig(): ObjectMapper {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    return this
}
