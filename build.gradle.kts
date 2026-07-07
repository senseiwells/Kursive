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

val modVersion = "0.3.0-alpha.1"
val releaseVersion = "${modVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion

allprojects {
    apply(plugin = "net.fabricmc.fabric-loom")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    repositories {
        mavenCentral()
        maven("https://maven.supersanta.me/snapshots")
        maven("https://maven.parchmentmc.org/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.isxander.dev/releases")
        maven("https://jitpack.io")
        maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
        mavenLocal()
    }

    val libs = rootProject.libs

    group = "me.senseiwells"

    dependencies {
        minecraft(libs.minecraft)

        implementation(libs.fabric.loader)
        implementation(libs.fabric.kotlin)
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
                        "minecraft_dependency" to replaceVersion(libs.versions.minecraft.get(), "x"),
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
    implementation(libs.fabric.api)
    implementation(libs.mod.menu)
    implementation(libs.yacl)

    include(libs.bundles.arcade)
    implementation(libs.bundles.arcade)

    include(implementation(libs.keybinds.get())!!)
    include(implementation(project(":kursive-api"))!!)

    localRuntime(libs.dev.auth)

    val kotlinVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")!!)
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")!!)
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlinVersion")!!)
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")!!)
    // shade(implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")!!)

    include(implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")!!)
    include("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-build-tools-api:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-daemon-embeddable:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:$kotlinVersion")
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runs {
        getByName("server") {
            runDirectory.set(file("run/server"))
        }

        getByName("client") {
            runDirectory.set(file("run/client"))
        }
    }
}


fun replaceVersion(version: String, patch: String): String {
    return version.replace(Regex("""^(\d+\.\d+)(\.\d+)?$"""), "$1.$patch")
}