package no.nav.tms.varsel.api.config

import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val corsAllowedOrigins: String = getEnvVar("CORS_ALLOWED_ORIGINS"),
    val corsAllowedSchemes: String = getEnvVar("CORS_ALLOWED_SCHEMES","https"),
    val eventHandlerURL: String = getEnvVar("EVENT_HANDLER_URL"),
    val eventhandlerClientId: String = getEnvVar("EVENTHANDLER_CLIENT_ID"),
)
