import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.fabricmc.fabric-loom")
    java
}

val libs = the<LibrariesForLibs>()

val modVersion: String = providers.gradleProperty("mod_version").get()
val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(libs.versions.java.get())

group = "me.senseiwells"
version = "${modVersion}+${libs.versions.minecraft.get()}"

repositories {
    mavenLocal()
    maven("https://maven.supersanta.me/snapshots")
    maven("https://maven.parchmentmc.org/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.isxander.dev/releases")
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.kotlin)
    implementation(libs.fabric.api)
}

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain {
        languageVersion.set(javaVersion)
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mapOf(
                "version" to project.version,
                "minecraft_dependency" to replaceVersion(libs.versions.minecraft.get(), "x"),
                "fabric_loader_dependency" to libs.versions.fabric.loader.get(),
                "fabric_api_dependency" to libs.versions.fabric.api.get(),
                "fabric_kotlin_dependency" to libs.versions.fabric.kotlin.get(),
            ))
        }
    }

    jar {
        from(rootProject.file("LICENSE")) {
            rename { "kursive-LICENSE" }
        }
    }
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }
}

private val minecraftVersionRegex = Regex("""^(\d+\.\d+)(\.\d+)?(?:-(pre|rc)-?(\d+))?$""")

fun replaceVersion(version: String, patch: String): String {
    val match = minecraftVersionRegex.matchEntire(version)
        ?: throw IllegalArgumentException("Unrecognised Minecraft version: $version")
    val (minor, patchVersion, type, number) = match.destructured
    if (type.isEmpty()) {
        return "$minor.$patch"
    }
    return "$minor$patchVersion-$type.$number"
}
