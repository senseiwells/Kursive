import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("kursive.common-conventions")
    `maven-publish`
}

version = "${providers.gradleProperty("mod_version").get()}+${libs.versions.minecraft.get()}"

dependencies {
    implementation(libs.bundles.arcade)
    implementation(libs.keybinds)
}

val buildKmcJar = tasks.register<ShadowJar>("buildKmcJar") {
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

    from(sourceSets.main.map { it.output })
    from(loom.namedMinecraftJars)
    from(project.configurations.modCompileClasspathMapped)
    configurations = listOf(
        project.configurations.minecraftClientRuntimeLibraries.get(),
        project.configurations.runtimeClasspath.get()
    )

    archiveBaseName = "kmc"
    archiveClassifier = ""
}

publishing {
    publications {
        create<MavenPublication>("kmc") {
            groupId = project.group.toString()
            artifactId = "kmc"

            artifact(buildKmcJar)
        }
    }
}

val updateDocumentedDependencies = tasks.register("updateDocumentedDependencies") {
    group = "documentation"
    description = "Updates the kmc version referenced in the documentation"

    val files = listOf(
        rootProject.file("docs/develop/index.md"),
        rootProject.file("docs/develop/creating-scripts.md"),
    )
    val coordinate = "me.senseiwells:kmc:${project.version}"
    val path = "me/senseiwells/kmc/${project.version}"

    inputs.property("coordinate", coordinate)
    outputs.files(files)

    doLast {
        for (file in files) {
            if (!file.exists()) {
                continue
            }
            file.writeText(
                file.readText()
                    .replace(Regex("""@file:DependsOn\("me\.senseiwells:kmc:[^"]*"\)"""), "@file:DependsOn(\"$coordinate\")")
                    .replace(Regex("""me/senseiwells/kmc/[^`]*`"""), "$path`")
            )
        }
    }
}

tasks.register("publishKmc") {
    group = "scripting"
    description = "Publishes the scripting jar to Maven Local"
    dependsOn(updateDocumentedDependencies)
    dependsOn(tasks.named("publishKmcPublicationToMavenLocal"))
}
