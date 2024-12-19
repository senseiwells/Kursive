import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    modImplementation(libs.arcade.event.registry.get())
    modImplementation(libs.arcade.events.client.get())
    modImplementation(libs.arcade.events.server.get())
}

tasks.register<ShadowJar>("mojangFatJar") {
    group = "scripting"
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

    archiveClassifier = "fat-mojang"
}