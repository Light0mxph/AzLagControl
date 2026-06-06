plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.azlagcontrol"
version = "1.0.0"
description = "AzLagControl - Professional lag control suite for Minecraft servers by AztrixDigitalStudio"

java {
    toolchain {
        // Java 21 (LTS) minimum — runs on Java 21, 25, and later releases
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API - primary target. Spigot-compatible API used throughout.
    // 1.21.4 API — targets 1.21.x; plugin loads on 1.21+ (including 1.26.x)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // PlaceholderAPI - optional integration
    compileOnly("me.clip:placeholderapi:2.11.5")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version, "description" to project.description)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("AzLagControl-${project.version}.jar")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }
}
