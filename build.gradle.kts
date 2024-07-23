import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.kotlin.logging)
}

application {
    applicationName = "dp-speider"
    mainClass.set("no.nav.dagpenger.speider.AppKt")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
