import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript { repositories { mavenCentral() } }

plugins {
    id("dagpenger.rapid-and-rivers")
}

application {
    applicationName = "dp-speider"
    mainClass.set("no.nav.dagpenger.speider.AppKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
}
