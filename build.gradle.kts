import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "me.centralhardware.telegram"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("dev.inmo:tgbotapi:20.0.0")
    implementation("com.github.centralhardware:telegram-bot-commons:ef17c6cc90")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("io.github.crackthecodeabhi:kreds:0.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.michael-bull.kotlin-retry:kotlin-retry:2.0.1")
    implementation("net.sourceforge:jwbf:3.1.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
    }
}