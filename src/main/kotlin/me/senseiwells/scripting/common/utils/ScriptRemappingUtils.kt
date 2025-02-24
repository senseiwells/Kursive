package me.senseiwells.scripting.common.utils

import com.google.gson.JsonObject
import kotlinx.io.IOException
import me.senseiwells.scripting.api.ScriptReflection
import me.senseiwells.scripting.common.EssentialScripting
import me.senseiwells.scripting.common.remapping.metadata.KotlinMetadataTinyRemapperExtensionImpl
import me.senseiwells.scripting.common.script.configuration.MappingType
import me.senseiwells.scripting.common.script.reflection.RemappedScriptReflection
import me.senseiwells.scripting.impl.CommonScriptingApi
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.impl.ModContainerImpl
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.*
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.tinyremapper.*
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.server.MinecraftServer
import net.minecraft.util.GsonHelper
import org.spongepowered.include.com.google.common.collect.HashMultimap
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.jar.JarFile
import kotlin.io.path.*

object ScriptRemappingUtils {
    private const val MOJANG_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    private const val YARN_META_URL = "https://meta.fabricmc.net/v2/versions/yarn"
    private const val YARN_MAVEN_URL = "https://maven.fabricmc.net/net/fabricmc/yarn"

    private val version = SharedConstants.getCurrentVersion().name
    private val mappings by lazy(ScriptRemappingUtils::createMappingTree)

    private var mappedMinecraftJars = EnumMap<_, Path>(MappingType::class.java)
    private var mappedModJars = HashMultimap.create<MappingType, Path>()
    private var unmappedModJars = ArrayList<Path>()

    fun getMappings(from: MappingType, to: MappingType = getCurrentMappings()): IMappingProvider {
        return TinyUtils.createMappingProvider(mappings, from.id, to.id)
    }

    fun getMappedMinecraftJar(type: MappingType): Path? {
        return mappedMinecraftJars[type]
    }

    fun getUnmappedMinecraftJar(): Path {
        return Path.of(MinecraftServer::class.java.protectionDomain.codeSource.location.toURI())
    }

    fun getMappedModJars(type: MappingType): Collection<Path> {
        return mappedModJars[type]
    }

    fun getUnmappedModJars(): Collection<Path> {
        return unmappedModJars
    }

    fun shouldRemap(type: MappingType): Boolean {
        return getCurrentMappings() != type
    }

    fun getCurrentMappings(): MappingType {
        return if (isCurrentIntermediary()) MappingType.Intermediary else MappingType.Mojang
    }

    fun createScriptReflection(from: MappingType): ScriptReflection {
        return RemappedScriptReflection(mappings, from)
    }

    private fun isCurrentIntermediary(): Boolean {
        return !FabricLoader.getInstance().isDevelopmentEnvironment
    }

    internal fun load(): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync({
            if (getIntermediary2Mojang2YarnPath().notExists()) {
                writeIntermediary2Mojang2YarnMappings()
            }
            createRemappedMinecraftJar(getCurrentMappings())
            for (entry in MappingType.named()) {
                if (entry != getCurrentMappings()) {
                    createRemappedMinecraftJar(entry)
                }
            }
            mapScriptingApi()
        }, Util.ioPool())
    }

    private fun mapScriptingApi() {
        val scriptingApi = FabricLoader.getInstance()
            .getModContainer(CommonScriptingApi.MOD_ID)
            .orElseThrow { IllegalStateException("Expected scripting-api to be present!") }
            as ModContainerImpl
        val scriptingApiJar = scriptingApi.codeSourcePaths.first()
        unmappedModJars.add(scriptingApiJar)
        for (named in MappingType.named()) {
            createRemappedModJar(CommonScriptingApi.MOD_ID, scriptingApiJar, named)
        }

        val includes = getScriptingApiIncludes(scriptingApi)
        for ((id, include) in includes) {
            unmappedModJars.add(include)
            for (name in MappingType.named()) {
                createRemappedModJar(id, include, name)
            }
        }
    }

    private fun getScriptingApiIncludes(scriptingApi: ModContainer): List<Pair<String, Path>> {
        if (!isCurrentIntermediary()) {
            return FabricLoader.getInstance().allMods.filter { container ->
                container.metadata.id.contains("arcade")
            }.map { it.metadata.id to it.origin.paths.first() }
        }
        val includes = ArrayList<Pair<String, Path>>()
        for (container in scriptingApi.containedMods) {
            container as ModContainerImpl
            val id = container.metadata.id
            try {
                includes.add(id to container.codeSourcePaths.first())
            } catch (e: IOException) {
                EssentialScripting.logger.error("Failed to copy mod '$id'")
            }
        }
        return includes
    }

    private fun createRemappedMinecraftJar(to: MappingType) {
        val from = getCurrentMappings()

        val input = getUnmappedMinecraftJar()
        if (from.id == to.id) {
            mappedMinecraftJars[to] = input
            return
        }

        val output = input.resolveSibling("${input.nameWithoutExtension}-mapped-${to.id}.jar")
        if (!output.exists()) {
            remapJar(input, output, from, to)
        }
        mappedMinecraftJars[to] = output
    }

    private fun createRemappedModJar(
        id: String,
        input: Path,
        to: MappingType
    ) {
        val from = getCurrentMappings()

        if (from.id == to.id) {
            mappedModJars.put(to, input)
            return
        }

        val output = FabricLoader.getInstance().gameDir
            .resolve(".fabric")
            .resolve("remappedJars")
            .resolve(id)
            .resolve("${input.nameWithoutExtension}-mapped-${to.id}.jar")
        if (!output.exists()) {
            remapJar(input, output, from, to)
        }
        mappedModJars.put(to, output)
    }

    internal fun remapJar(
        input: Path,
        output: Path,
        from: MappingType,
        to: MappingType,
    ) {
        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createMappingProvider(mappings, from.id, to.id))
            .extension(KotlinMetadataTinyRemapperExtensionImpl)
            .build()
        try {
            remapper.readClassPath(getMappedMinecraftJar(from))
            OutputConsumerPath.Builder(output).build().use { consumer ->
                consumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(input)
                remapper.apply(consumer)
            }
        } catch (e: Exception) {
            EssentialScripting.logger.error("Exception occurred", e)
        } finally {
            remapper.finish()
        }
    }

    private fun createMappingTree(): MemoryMappingTree {
        val tree = MemoryMappingTree()
        val path = getIntermediary2Mojang2YarnPath()
        if (path.notExists()) {
            writeIntermediary2Mojang2YarnMappings()
        }
        if (path.isReadable()) {
            MappingReader.read(path, tree)
        }
        return tree
    }

    private fun writeIntermediary2Mojang2YarnMappings() {
        val tree = MemoryMappingTree()
        val yarn = getOrDownloadMappings(getIntermediary2YarnMappingsPath(), this::downloadYarnMappings)
        val mojang = getOrDownloadMappings(getIntermediary2MojangMappingsPath(), this::downloadMojangMappings)
        if (mojang == null || yarn == null) {
            return
        }
        MappingReader.read(mojang, tree)
        MappingReader.read(yarn, tree)
        val writer = MappingWriter.create(
            getIntermediary2Mojang2YarnPath(),
            MappingFormat.TINY_2_FILE
        )
        val completer = MappingNsCompleter(writer, mapOf("yarn" to "intermediary"))
        tree.accept(completer)
    }

    private fun getOrDownloadMappings(
        location: Path,
        download: () -> Unit
    ): Path? {
        if (location.notExists()) {
            download.invoke()
            if (location.notExists()) {
                EssentialScripting.logger.error("Failed to load mappings at $location")
                return null
            }
        }
        return location
    }

    private fun downloadMojangMappings() {
        val mojang = getMojangMappingsUrl() ?: return
        val intermediary = "https://github.com/FabricMC/intermediary/raw/master/mappings/$version.tiny"

        val renames = mapOf("source" to "mojang", "target" to "official")
        val tree = MemoryMappingTree()
        val renamer = MappingNsRenamer(tree, renames)
        try {
            NetworkingUtils.fetchAsReader(intermediary) { reader ->
                MappingReader.read(reader, renamer)
            }
            NetworkingUtils.fetchAsReader(mojang) { reader ->
                MappingReader.read(reader, MappingSourceNsSwitch(renamer, "target"))
            }

            val path = getIntermediary2MojangMappingsPath()
            val writer = MappingWriter.create(path, MappingFormat.TINY_2_FILE)
            val reorder = MappingDstNsReorder(writer, listOf("mojang"))
            val switcher = MappingSourceNsSwitch(reorder, "intermediary", true)
            tree.accept(switcher)
        } catch (e: IOException) {
            EssentialScripting.logger.error("Failed to download mojang mappings", e)
        }
    }

    private fun getMojangMappingsUrl(): String? {
        val url = getMojangMetaUrl() ?: return null
        val meta = NetworkingUtils.fetchAsJsonObject(url) ?: return null
        val downloads = GsonHelper.getAsJsonObject(meta, "downloads")
        val mappings = GsonHelper.getAsJsonObject(downloads, "client_mappings")
        return GsonHelper.getAsString(mappings, "url")
    }

    private fun getMojangMetaUrl(): String? {
        val manifest = NetworkingUtils.fetchAsJsonObject(MOJANG_MANIFEST_URL) ?: return null
        val versions = GsonHelper.getAsJsonArray(manifest, "versions")
        for (version in versions) {
            version as JsonObject
            val id = GsonHelper.getAsString(version, "id")
            if (id == ScriptRemappingUtils.version) {
                return GsonHelper.getAsString(version, "url")
            }
        }
        return null
    }

    private fun downloadYarnMappings() {
        val url = getYarnJarUrl() ?: return

        val renames = mapOf("named" to "yarn")
        val tree = MemoryMappingTree()
        val renamer = MappingNsRenamer(tree, renames)
        try {
            val path = getIntermediary2YarnMappingsPath()
            NetworkingUtils.fetchAsStream(url) { stream ->
                val tmp = Files.createTempFile(path.parent, path.nameWithoutExtension, null)
                try {
                    tmp.outputStream().use { output ->
                        stream.transferTo(output)
                    }
                    val jar = JarFile(tmp.toFile())
                    val entry = jar.getJarEntry("mappings/mappings.tiny")
                        ?: throw IllegalStateException("Failed to find mappings in yarn jar")
                    jar.getInputStream(entry).reader().use { reader ->
                        MappingReader.read(reader, renamer)
                    }
                } finally {
                    tmp.deleteIfExists()
                }
            }

            val writer = MappingWriter.create(path, MappingFormat.TINY_2_FILE)
            tree.accept(MappingCommentIgnorer(writer))
        } catch (e: IOException) {
            EssentialScripting.logger.error("Failed to download yarn mappings", e)
        }
    }

    private fun getYarnJarUrl(): String? {
        val meta = NetworkingUtils.fetchAsJsonArray("$YARN_META_URL/$version") ?: return null
        val entry = meta.maxByOrNull { entry ->
            entry.asJsonObject.get("build").asInt
        } ?: return null
        val version = entry.asJsonObject.get("version").asString
        return "$YARN_MAVEN_URL/$version/yarn-$version-v2.jar"
    }


    private fun getIntermediary2Mojang2YarnPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("intermediary2mojang2yarn")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getIntermediary2MojangMappingsPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("intermediary2mojang")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getIntermediary2YarnMappingsPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("intermediary2yarn")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private class MappingCommentIgnorer(next: MappingVisitor?): ForwardingMappingVisitor(next) {
        override fun visitComment(targetKind: MappedElementKind?, comment: String?) {

        }
    }
}