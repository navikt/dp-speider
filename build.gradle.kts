import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
    alias(libs.plugins.shadow.jar)
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation("io.prometheus:prometheus-metrics-core:1.3.4")
    testImplementation(libs.rapids.and.rivers.test)
    implementation(libs.kotlin.logging)
}

application {
    applicationName = "dp-speider"
    mainClass.set("no.nav.dagpenger.speider.AppKt")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
