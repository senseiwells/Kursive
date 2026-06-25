import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val apiVersion = "0.2.0-alpha.2"
val releaseVersion = "${apiVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion

dependencies {
    implementation(libs.fabric.api)

    include(libs.bundles.arcade)
    implementation(libs.bundles.arcade)
}

tasks.register<ShadowJar>("buildKmcJar") {
    group = "scripting"
    description = "Builds a fat jar containing Minecraft and runtime dependencies for use in Kotlin scripts"
    isZip64 = true

    exclude("_COROUTINE/*")
    exclude("com/ibm/**")
    exclude("com/sun/**")
    exclude("com/jcraft/**")
    exclude("ui/**")
    exclude("META-INF/jars/*")
    exclude("assets/**")
    exclude("data/**")
    exclude("*-refmap.json")
    exclude("*.mixins.json")
    exclude("*.accesswidener")
    exclude("*.classtweaker")
    exclude("DebugProbesKt.bin")
    exclude("fabric.mod.json")
    exclude("fabric-installer.json")
    exclude("fabric-installer.launchwrapper.json")

    from(sourceSets.main.get().output)
    from(loom.namedMinecraftJars)
    from(project.configurations.modCompileClasspathMapped)
    configurations = listOf(
        project.configurations.minecraftClientRuntimeLibraries.get(),
        project.configurations.runtimeClasspath.get()
    )

    archiveBaseName = "kmc"
    archiveClassifier = ""
    archiveVersion = project.version.toString()
}

publishing {
    publications {
        create<MavenPublication>("kmc") {
            groupId = "me.senseiwells"
            artifactId = "kmc"

            artifact(tasks.named("buildKmcJar"))
        }
    }
}

tasks.register("publishKmc") {
    group = "scripting"
    description = "Publishes the scripting jar to Maven Local"
    dependsOn("publishKmcPublicationToMavenLocal")
}