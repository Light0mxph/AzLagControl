plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.azlagcontrol"
version = "1.0.0"
description = "AzLagControl - Professional lag control suite for Minecraft servers by AztrixDigitalStudio"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API - primary target. Spigot-compatible API used throughout.
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // PlaceholderAPI - optional integration
    compileOnly("me.clip:placeholderapi:2.11.5")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
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
