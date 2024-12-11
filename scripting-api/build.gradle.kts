import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    modApi(libs.arcade.event.registry)
    modApi(libs.arcade.events.client)
    modApi(libs.arcade.events.server)
    modImplementation(libs.arcade.utils)
}

tasks.register<ShadowJar>("fatJar") {
    isZip64 = true

    exclude("_COROUTINE/*")
    exclude("kotlin/**")
    exclude("com/ibm/**")
    exclude("com/sun/**")
    exclude("ui/**")
    exclude("META-INF/jars/*")
    exclude("assets/**")
    exclude("data/**")
    exclude("*-refmap.json")
    exclude("*.mixins.json")
    exclude("*.accesswidener")
    exclude("DebugProbesKt.bin")
    exclude("fabric.mod.json")
    exclude("fabric-installer.json")
    exclude("fabric-installer.launchwrapper.json")

    from(sourceSets.main.get().output)
    from(loom.namedMinecraftJars)
    from(project.configurations.modCompileClasspathMapped)
    configurations = listOf(
        project.configurations.minecraftClientRuntimeLibraries.get()
    )

    archiveClassifier = "fat"
}