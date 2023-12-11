buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.kotlin.logging)
}

application {
    applicationName = "dp-speider"
    mainClass.set("no.nav.dagpenger.speider.AppKt")
}
