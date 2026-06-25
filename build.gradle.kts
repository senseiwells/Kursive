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

val modVersion = "0.2.0-alpha.1"
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

    include(implementation("org.jetbrains.kotlin:kotlin-scripting-common")!!) // fine
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")!!) // fine
    include(implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")!!) // fine
    shade(implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")!!) // kotlinx.coroutines, org.intellij, org.jetbrains
    // shade(implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")!!)
    include(implementation(libs.kotlin.metadata.get())!!)
}

tasks {
    shadowJar {
        destinationDirectory.set(File("./build/devlibs"))

        exclude { element ->
            element.path.startsWith("kotlin/") && !element.path.startsWith("kotlin/script/")
        }
        exclude("kotlinx/coroutines/**")
        exclude("messages/**")

        // from("LICENSE")

        configurations = listOf(shade)
        archiveClassifier = "shaded"
    }
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