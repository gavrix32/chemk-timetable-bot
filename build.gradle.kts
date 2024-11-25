plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.telegram:telegrambots-longpolling:7.10.0")
    implementation("org.telegram:telegrambots-client:7.10.0")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

kotlin {
    jvmToolchain(21)
}