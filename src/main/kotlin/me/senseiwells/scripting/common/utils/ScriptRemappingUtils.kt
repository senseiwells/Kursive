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
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.tinyremapper.IMappingProvider
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.server.MinecraftServer
import net.minecraft.util.GsonHelper
import org.spongepowered.include.com.google.common.collect.HashMultimap
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
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
        return mappings.provider(from.id, to.id, false)
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

    internal fun load() {
        Util.ioPool().execute {
            writeIntermediary2Mojang2YarnMappings()
            for (named in MappingType.named()) {
                createRemappedMinecraftJar(named)
            }
            mapScriptingApi()
        }
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
            .withMappings(mappings.provider(from.id, to.id, true))
            .extension(KotlinMetadataTinyRemapperExtensionImpl)
            .build()
        try {
            OutputConsumerPath.Builder(output).build().use { consumer ->
                consumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(input)
                remapper.apply(consumer)
            }
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
        val renames = mapOf("source" to "mojang", "target" to "official", "named" to "yarn")
        val tree = MemoryMappingTree()
        val renamer = MappingNsRenamer(tree, renames)
        val intermediary = getOrDownloadMappings(getOfficial2IntermediaryMappingsPath(), this::downloadIntermediaryMappings)
        val yarn = getOrDownloadMappings(getIntermediary2YarnMappingsPath(), this::downloadYarnMappings)
        val mojang = getOrDownloadMappings(getMojang2OfficialMappingsPath(), this::downloadMojangMappings)
        if (intermediary == null || mojang == null || yarn == null) {
            return
        }
        // I'm sure there's a better way of doing this
        MappingReader.read(mojang, renamer)
        MappingReader.read(intermediary, renamer)
        val copy = MemoryMappingTree()
        val switcher = MappingSourceNsSwitch(MappingNsRenamer(copy, renames), "intermediary", true)
        tree.accept(switcher)
        MappingReader.read(yarn, MappingCommentIgnorer(copy))
        val writer = MappingWriter.create(
            getIntermediary2Mojang2YarnPath(),
            MappingFormat.TINY_2_FILE
        )
        val completer = MappingNsCompleter(writer, mapOf("yarn" to "intermediary"))
        val reorder = MappingNsRenamer(MappingDstNsReorder(completer, listOf("mojang", "yarn")), renames)
        copy.accept(reorder)
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
        val url = getMojangMappingsUrl() ?: return
        try {
            NetworkingUtils.fetchAsReader(url) { reader ->
                val path = getMojang2OfficialMappingsPath()
                val writer = MappingWriter.create(path, MappingFormat.TINY_2_FILE)
                MappingReader.read(reader, writer)
            }
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
        try {
            NetworkingUtils.fetchAsStream(url) { stream ->
                val path = getIntermediary2YarnMappingsPath()
                val tmp = Files.createTempFile(path.parent, path.nameWithoutExtension, null)
                try {
                    tmp.outputStream().use { output ->
                        stream.transferTo(output)
                    }
                    val jar = JarFile(tmp.toFile())
                    val entry = jar.getJarEntry("mappings/mappings.tiny")
                        ?: throw IllegalStateException("Failed to find mappings in yarn jar")
                    jar.getInputStream(entry).use { input ->
                        path.outputStream().use { output ->
                            input.transferTo(output)
                        }
                    }
                } finally {
                    tmp.deleteIfExists()
                }
            }
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

    private fun downloadIntermediaryMappings() {
        val url = "https://github.com/FabricMC/intermediary/raw/master/mappings/$version.tiny"
        try {
            NetworkingUtils.fetchAsStream(url) { input ->
                val path = getOfficial2IntermediaryMappingsPath()
                path.outputStream().use { output ->
                    input.transferTo(output)
                }
            }
        } catch (e: IOException) {
            EssentialScripting.logger.error("Failed to download intermediary mappings", e)
        }
    }

    private fun getIntermediary2Mojang2YarnPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("intermediary2mojang2yarn")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getMojang2OfficialMappingsPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("mojang2official")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getIntermediary2YarnMappingsPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("intermediary2yarn")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getOfficial2IntermediaryMappingsPath(): Path {
        return EssentialScripting.configDirectory().resolve("mappings")
            .resolve("official2intermediary")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun MappingTree.provider(from: String, to: String, remapLocals: Boolean): IMappingProvider {
        return IMappingProvider { acceptor ->
            val fromId = this.getNamespaceId(from)
            val toId = this.getNamespaceId(to)
            for (classDef in this.classes) {
                val className = classDef.getName(fromId) ?: continue

                val dstClassName = classDef.getName(toId) ?: className
                acceptor.acceptClass(className, dstClassName)

                for (field in classDef.fields) {
                    val fieldName = field.getName(fromId) ?: continue

                    val dstFieldName = field.getName(toId) ?: fieldName
                    acceptor.acceptField(
                        IMappingProvider.Member(className, fieldName, field.getDesc(fromId)), dstFieldName
                    )
                }

                for (method in classDef.methods) {
                    val methodName = method.getName(fromId) ?: continue

                    val dstMethodName = method.getName(toId) ?: methodName
                    val methodIdentifier = IMappingProvider.Member(className, methodName, method.getDesc(fromId))
                    acceptor.acceptMethod(methodIdentifier, dstMethodName)

                    if (!remapLocals) {
                        continue
                    }

                    for (parameter in method.args) {
                        val name = parameter.getName(toId) ?: continue

                        acceptor.acceptMethodArg(methodIdentifier, parameter.lvIndex, name)
                    }

                    for (localVariable in method.vars) {
                        acceptor.acceptMethodVar(
                            methodIdentifier, localVariable.lvIndex,
                            localVariable.startOpIdx, localVariable.lvtRowIndex,
                            localVariable.getName(toId)
                        )
                    }
                }
            }
        }
    }

    private class MappingCommentIgnorer(next: MappingVisitor?): ForwardingMappingVisitor(next) {
        override fun visitComment(targetKind: MappedElementKind?, comment: String?) {

        }
    }
}