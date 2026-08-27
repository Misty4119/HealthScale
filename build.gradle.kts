plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.9"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "noietime"
version = "2.1.1"

repositories {
    mavenCentral()
    maven("https://maven.canvasmc.io/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Compile against the exact API bundled with Canvas 26.2 build 923.
    compileOnly("io.canvasmc.canvas:canvas-api:26.2.build.923-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }

    shadowJar {
        archiveClassifier = ""
        archiveBaseName = "HealthScale"
        archiveVersion = version.toString()
        minimize()
    }

    jar {
        // Disable the plain jar; shadowJar is the output artifact
        enabled = false
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
