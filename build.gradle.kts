import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.serialization").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)
    application
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(DittNAVCommonLib.utils)
    implementation(Ktor2.Client.core)
    implementation(Ktor2.Client.apache)
    implementation(Ktor2.Client.contentNegotiation)
    implementation(Ktor2.Server.core)
    implementation(Ktor2.Server.netty)
    implementation(Ktor2.Server.auth)
    implementation(Ktor2.Server.contentNegotiation)
    implementation(Ktor2.Server.statusPages)
    implementation(Ktor2.Server.metricsMicrometer)
    implementation(Ktor2.Server.defaultHeaders)
    implementation(Ktor2.Server.cors)
    implementation(TmsKtorTokenSupport.tokenXValidation)
    implementation(TmsKtorTokenSupport.idportenSidecar)
    implementation(TmsKtorTokenSupport.authenticationInstaller)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(KotlinLogging.logging)
    implementation(Logstash.logbackEncoder)
    implementation(Logback.classic)
    implementation("io.ktor:ktor-server-call-logging:2.1.1")
    implementation(Ktor2.Serialization.kotlinX)
    implementation(Micrometer.registryPrometheus)

    testImplementation(kotlin("test"))
    testImplementation(Kotest.assertionsCore)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Ktor2.Test.serverTestHost)
    testImplementation(Ktor2.Test.clientMock)
    implementation(TmsKtorTokenSupport.tokenXValidationMock)
    implementation(TmsKtorTokenSupport.idportenSidecarMock)
    testImplementation(Mockk.mockk)
}

application {
    mainClass.set("no.nav.tms.varsel.api.ApplicationKt")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}

// TODO: Fjern følgende work around i ny versjon av Shadow-pluginet:
// Skal være løst i denne: https://github.com/johnrengelman/shadow/pull/612
project.setProperty("mainClassName", application.mainClass.get())
apply(plugin = Shadow.pluginId)
