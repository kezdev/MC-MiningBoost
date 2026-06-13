plugins {
    java
}

group = "dev.kezhall"
version = "1.2.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // Paper API for Minecraft 26.1.2. "compileOnly" because the server
    // already provides it at runtime. ".build.+" = latest build of 26.1.2.
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

java {
    // Paper 26.1.2 requires Java 25.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.processResources {
    // Lets you use ${version} inside plugin.yml so it stays in sync with above.
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// Convenience: copy the built jar straight into your server's plugins folder.
// Default points at the server on your Desktop. Override without editing this file:
//   ./gradlew deployToServer -PserverPluginsDir=/some/other/plugins
// or add a `serverPluginsDir=...` line to ~/.gradle/gradle.properties (never committed).
val serverPluginsDir = (project.findProperty("serverPluginsDir") as String?)
    ?: "${System.getProperty("user.home")}/Desktop/Minecraft Server 26/plugins"

val deployToServer by tasks.registering(Copy::class) {
    group = "deployment"
    description = "Build the jar and copy it into the server's plugins folder."
    dependsOn(tasks.jar)
    from(tasks.jar.flatMap { it.archiveFile })
    into(serverPluginsDir)
}
