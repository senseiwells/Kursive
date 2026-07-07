import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val apiVersion = "0.3.0-alpha.1"
val releaseVersion = "${apiVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion

dependencies {
    implementation(libs.fabric.api)

    implementation(libs.bundles.arcade)
    implementation(libs.keybinds)
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

afterEvaluate {
    updateDocumentedDependencies("../docs/developing/creating-a-script.md")
}

private fun Project.updateDocumentedDependencies(path: String) {
    val file = file(path)
    if (file.exists()) {
        val document = file.readText()
            .replace(Regex("""@file:DependsOn\("me\.senseiwells:kmc:.*"\)"""), "@file:DependsOn(\"me.senseiwells:kmc:$releaseVersion\")")
            .replace(Regex("""me/senseiwells/kmc/.*`"""), "me/senseiwells/kmc/$releaseVersion`")
        file.writeText(document)
    }
}