plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:5.11.0")

}
