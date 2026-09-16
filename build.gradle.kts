import me.modmuss50.mpp.ReleaseType

plugins {
    id("kursive.common-conventions")
    alias(libs.plugins.mod.publish)
    alias(libs.plugins.shadow) apply false
}

val modVersion = providers.gradleProperty("mod_version").get()
val minecraftVersion = libs.versions.minecraft.get()

@Suppress("AvoidDuplicateDependencies", "RedundantSuppression")
dependencies {
    implementation(libs.mod.menu)
    implementation(libs.yacl)

    implementation(libs.bundles.arcade)
    include(libs.bundles.arcade)

    include(implementation(libs.keybinds.get())!!)
    include(implementation(projects.kursiveApi)!!)

    implementation(libs.bundles.kotlin.scripting)
    include(libs.bundles.kotlin.scripting)
    include(libs.bundles.kotlin.scripting.runtime)

    localRuntime(libs.dev.auth)
}

loom {
    runs {
        named("server") {
            runDirectory.set(layout.projectDirectory.dir("run/server"))
        }

        named("client") {
            runDirectory.set(layout.projectDirectory.dir("run/client"))
        }
    }
}

publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    changelog.set(
        """
        - Fix not having server scripting permissions in singleplayer
        """.trimIndent()
    )
    type = when {
        "alpha" in modVersion -> ReleaseType.ALPHA
        "beta" in modVersion -> ReleaseType.BETA
        else -> ReleaseType.STABLE
    }
    modLoaders.add("fabric")

    displayName = "Kursive $modVersion for $minecraftVersion"
    version = project.version.toString()

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_API_KEY")
        projectId = "y3pxdyS7"
        minecraftVersions.add(minecraftVersion)

        projectDescription = createProjectDescription()

        requires("P7dR8mSH")
        requires("Ha28R6CL")
        optional("1eAoo2KR")
    }
}

fun createProjectDescription(): String {
    var description = rootProject.file("README.md").readText()

    description = description.replace(Regex("""\[!([A-Z]+)]""")) { match ->
        val type = match.groupValues[1]
        "**${type.lowercase().replaceFirstChar(Char::uppercase)}:**"
    }
    description = description.replace(Regex("""<!-- #(end)?region [\w-]+ -->\n?"""), "")
    val raw = "https://raw.githubusercontent.com/senseiwells/Kursive/HEAD/"
    description = description.replace(Regex("""(src|href)="\./"""), "$1=\"$raw")
        .replace(Regex("""]\((?!https?://|#)\.?/?"""), "]($raw")

    return description
}