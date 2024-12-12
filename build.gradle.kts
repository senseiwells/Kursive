
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.pathString

plugins {
    val jvmVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]

    kotlin("jvm").version(jvmVersion)
    kotlin("plugin.serialization").version(jvmVersion)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.mod.publish)
    alias(libs.plugins.shadow)
    `maven-publish`
    java
}

val shade: Configuration by configurations.creating

val modVersion = "0.1.0-alpha.16"
val releaseVersion = "${modVersion}+${libs.versions.minecraft.get()}"

allprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.parchmentmc.org/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.isxander.dev/releases")
        maven("https://maven.supersanta.me/snapshots")
        maven("https://jitpack.io")
    }

    val libs = rootProject.libs

    version = releaseVersion
    group = "me.senseiwells"

    dependencies {
        minecraft(libs.minecraft)
        @Suppress("UnstableApiUsage")
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
        })

        modImplementation(libs.fabric.loader)
        modImplementation(libs.fabric.kotlin)
    }

    java {
        withSourcesJar()
    }

    tasks {
        processResources {
            inputs.property("version", project.version)
            filesMatching("fabric.mod.json") {
                expand(
                    mutableMapOf(
                        "version" to project.version,
                        "minecraft_dependency" to libs.versions.minecraft.get().replaceAfterLast('.', "x"),
                        "yacl_dependency" to libs.versions.yacl.get(),
                        "fabric_loader_dependency" to libs.versions.fabric.loader.get(),
                        "fabric_api_dependency" to libs.versions.fabric.api.get(),
                        "fabric_kotlin_dependency" to libs.versions.fabric.kotlin.get()
                    )
                )
            }
        }
    }
}

dependencies {
    modImplementation(libs.fabric.api)
    modImplementation(libs.mod.menu)
    modImplementation(libs.yacl)

    include(modImplementation(libs.keybinds.get())!!)
    include(implementation(project(path = ":scripting-api", configuration = "namedElements"))!!)

    include(implementation("org.jetbrains.kotlin:kotlin-scripting-common")!!) // fine
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")!!) // fine
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")!!) // fine
    shade(implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")!!) // kotlinx.coroutines, org.intellij, org.jetbrains
    // shade(implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")!!)
    include(implementation(libs.mappingio.get())!!)
    include(implementation(libs.fabric.tiny.remapper.get())!!)
    include(implementation(libs.kotlin.metadata.get())!!)
}

tasks {
    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
    }

    shadowJar {
        destinationDirectory.set(File("./build/devlibs"))

        // from("LICENSE")

        configurations = listOf(shade)
        archiveClassifier = "shaded"
    }

    register("yarnFatJar") {
        group = "scripting"
        val scriptingApi = project(":scripting-api")
        val mojangFatJar: Task = scriptingApi.tasks.getByName("mojangFatJar")
        val libs = scriptingApi.layout.buildDirectory.get().asFile.toPath().resolve("libs")
        runClient.get().args(
            "--remap-input-jar", libs.resolve("scripting-api-${version}-fat-mojang.jar").pathString,
            "--remap-output-jar", libs.resolve("scripting-api-${version}-fat-yarn.jar").pathString,
            "--remap-from", "mojang",
            "--remap-to", "yarn"
        )
        doFirst { mojangFatJar.actions.forEach { it.execute(mojangFatJar) } }
        doLast { runClient.get().exec() }
    }

    register("generateFatJars") {
        group = "scripting"
        dependsOn("yarnFatJar")
        val scriptingApi = project(":scripting-api")
        val libs = scriptingApi.layout.buildDirectory.get().asFile.toPath().resolve("libs")
        val mojang = libs.resolve("scripting-api-${version}-fat-mojang.jar")
        val yarn = libs.resolve("scripting-api-${version}-fat-yarn.jar")
        val build = layout.buildDirectory.get().asFile.toPath()
            .resolve("scripting-jars")
            .createDirectories()
        doLast {
            mojang.copyTo(build.resolve(mojang.name), overwrite = true)
            yarn.copyTo(build.resolve(yarn.name), overwrite = true)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}