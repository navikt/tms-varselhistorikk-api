plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        create("tms-bundle-jars") {
            id = "no.nav.tms-bundle-jars"
            implementationClass = "BundleJars"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
