plugins {
    java
}

group = "com.straight8.rambeau"
version = "2.0.0"
description = "List installed plugins and versions alphabetically"

val buildNumber = "003"
val javaTarget = "25"
val paperTarget = "26.1.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.20-alpha")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.26.1")
    compileOnly("me.clip:placeholderapi:2.12.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xdiags:verbose"))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf(
        "version" to project.version,
        "description" to project.description,
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set("1MB-PluginVersions-v${project.version}-$buildNumber-j$javaTarget-$paperTarget.jar")
    destinationDirectory.set(layout.projectDirectory.dir("builds/libs"))
}

tasks.clean {
    delete(layout.projectDirectory.dir("builds"))
}
