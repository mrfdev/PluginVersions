plugins {
    java
}

group = "com.straight8.rambeau"
version = "2.0.0"
description = "List installed plugins and versions alphabetically"

val buildNumber = "005"
val javaTarget = "25"
val paperTarget = "26.1.2"
val paperApiVersion = "26.1.2.build.20-alpha"
val pluginApiCompatibility = "1.21.11"
val pluginName = project.name
val pluginVersion = project.version.toString()
val pluginJarName = "1MB-PluginVersions-v$pluginVersion-$buildNumber-j$javaTarget-$paperTarget.jar"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
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
        "apiVersion" to pluginApiCompatibility,
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set(pluginJarName)
    destinationDirectory.set(layout.projectDirectory.dir("libs"))
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Build-Number" to buildNumber,
            "Java-Target" to javaTarget,
            "Paper-API-Compile-Version" to paperApiVersion,
            "Plugin-API-Compatibility" to pluginApiCompatibility,
        )
    }
}

tasks.clean {
    delete(layout.projectDirectory.dir("builds"))
}

tasks.register("printBuildConfig") {
    group = "help"
    description = "Prints the plugin build and compatibility metadata."
    doLast {
        println("Plugin: $pluginName $pluginVersion")
        println("Build number: $buildNumber")
        println("Java target: $javaTarget")
        println("Compiles against Paper API: $paperApiVersion")
        println("Declares plugin api-version: $pluginApiCompatibility")
        println("Jar output: libs/$pluginJarName")
    }
}
