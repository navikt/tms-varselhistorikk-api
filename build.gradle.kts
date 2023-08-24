import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.serialization").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)
    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(DittNAVCommonLib.utils)
    implementation(Ktor.Client.core)
    implementation(Ktor.Client.apache)
    implementation(Ktor.Client.contentNegotiation)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.auth)
    implementation(Ktor.Server.contentNegotiation)
    implementation(Ktor.Server.statusPages)
    implementation(Ktor.Server.metricsMicrometer)
    implementation(Ktor.Server.defaultHeaders)
    implementation(Ktor.Server.cors)
    implementation(TmsKtorTokenSupport.idportenSidecar)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(KotlinLogging.logging)
    implementation(Logstash.logbackEncoder)
    implementation(Ktor.Serialization.kotlinX)
    implementation(Micrometer.registryPrometheus)
    implementation(Prometheus.logback)
    implementation(KtorClientEncoding.clientEncoding)
    implementation(TmsCommonLib.commonLib)

    testImplementation(kotlin("test"))
    testImplementation(Kotest.assertionsCore)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Ktor.Test.serverTestHost)
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
