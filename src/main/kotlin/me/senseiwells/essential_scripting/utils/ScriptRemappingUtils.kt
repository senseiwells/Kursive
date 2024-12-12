package me.senseiwells.essential_scripting.utils

import com.google.gson.JsonObject
import kotlinx.io.IOException
import me.senseiwells.essential_scripting.EssentialScripting
import me.senseiwells.essential_scripting.EssentialScriptingConfig
import me.senseiwells.essential_scripting.script.configuration.MappingType
import me.senseiwells.scripting.impl.CommonScriptingApi
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.adapter.MappingNsRenamer
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
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
import java.nio.file.Path
import java.util.*
import java.util.jar.JarFile
import java.util.zip.GZIPInputStream
import kotlin.io.path.*

object ScriptRemappingUtils {
    private const val MOJANG_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    private const val YARN_META_URL = "https://meta.fabricmc.net/v2/versions/yarn"
    private const val YARN_MAVEN_URL = "https://maven.fabricmc.net/net/fabricmc/yarn"

    private val version = SharedConstants.getCurrentVersion().name
    private val mappings by lazy(ScriptRemappingUtils::createMappingTree)

    private var mappedMinecraftJars = EnumMap<_, Path>(MappingType::class.java)
    private var mappedModJars = HashMultimap.create<MappingType, Path>()

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
        return this.mappedModJars[type]
    }

    fun shouldRemap(type: MappingType): Boolean {
        return getCurrentMappings() != type
    }

    fun isCurrentIntermediary(): Boolean {
        return !FabricLoader.getInstance().isDevelopmentEnvironment
    }

    private fun getCurrentMappings(): MappingType {
        return if (isCurrentIntermediary()) MappingType.Intermediary else MappingType.Mojang
    }

    internal fun load() {
        Util.ioPool().execute {
            this.writeIntermediary2Mojang2YarnMappings()
            for (named in MappingType.named()) {
                this.createRemappedMinecraftJar(named)
            }
            this.mapScriptingApi()
        }
    }

    private fun mapScriptingApi() {
        val scriptingApiJar = Path.of(
            CommonScriptingApi::class.java.protectionDomain.codeSource.location.toURI()
        )
        for (named in MappingType.named()) {
            this.createRemappedModJar(CommonScriptingApi.MOD_ID, scriptingApiJar, named)
        }

        val includes = this.getScriptingApiIncludes(scriptingApiJar)
        for ((id, include) in includes) {
            for (name in MappingType.named()) {
                this.createRemappedModJar(id, include, name)
            }
        }
    }

    private fun getScriptingApiIncludes(scriptingApiJar: Path): List<Pair<String, Path>> {
        if (!this.isCurrentIntermediary()) {
            return FabricLoader.getInstance().allMods.filter { container ->
                container.metadata.id.contains("arcade")
            }.map { it.metadata.id to it.origin.paths.first() }
        }
        val scriptingApi = FabricLoader.getInstance()
            .getModContainer(CommonScriptingApi.MOD_ID)
            .orElseThrow { IllegalStateException("Expected scripting-api to be present!") }
        val includes = ArrayList<Pair<String, Path>>()
        JarFile(scriptingApiJar.toFile()).use { jar ->
            for (container in scriptingApi.containedMods) {
                val id = container.metadata.id
                try {
                    includes.add(id to this.copyIncludedJar(jar, container))
                } catch (e: IOException) {
                    EssentialScripting.logger.error("Failed to copy mod '$id'")
                }
            }
        }
        return includes
    }

    private fun createRemappedMinecraftJar(to: MappingType) {
        val from = this.getCurrentMappings()

        val input = this.getUnmappedMinecraftJar()
        if (from.id == to.id) {
            this.mappedMinecraftJars[to] = input
            return
        }

        val output = input.resolveSibling("${input.nameWithoutExtension}-mapped-${to.id}.jar")
        if (!output.exists()) {
            this.remapJar(input, output, from, to)
        }
        this.mappedMinecraftJars[to] = output
    }

    private fun createRemappedModJar(
        id: String,
        input: Path,
        to: MappingType
    ) {
        val from = this.getCurrentMappings()

        if (from.id == to.id) {
            this.mappedModJars.put(to, input)
            return
        }

        val output = FabricLoader.getInstance().gameDir
            .resolve(".fabric")
            .resolve("remappedJars")
            .resolve(id)
            .resolve("${input.nameWithoutExtension}-mapped-${to.id}.jar")
        if (!output.exists()) {
            this.remapJar(input, output, from, to)
        }
        this.mappedModJars.put(to, output)
    }

    internal fun remapJar(
        input: Path,
        output: Path,
        from: MappingType,
        to: MappingType,
    ) {
        val remapper = TinyRemapper.newRemapper()
            .withMappings(this.mappings.provider(from.id, to.id, true))
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

    private fun copyIncludedJar(jar: JarFile, container: ModContainer): Path {
        val location = container.origin.parentSubLocation
        val entry = jar.getJarEntry(location)
        val path = FabricLoader.getInstance().gameDir
            .resolve(".fabric")
            .resolve("remappedJars")
            .resolve(container.metadata.id)
            .resolve(location.substringAfterLast('/'))
        if (!path.exists()) {
            path.outputStream().use { output ->
                jar.getInputStream(entry).use { input ->
                    input.transferTo(output)
                }
            }
        }
        return path
    }

    private fun createMappingTree(): MemoryMappingTree {
        val tree = MemoryMappingTree()
        val path = getIntermediary2Mojang2Yarn()
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
        val yarn = getOrDownloadYarnMappings()
        val mojang = getOrDownloadMojangMappings()
        if (mojang == null || yarn == null) {
            return
        }
        MappingReader.read(yarn, renamer)
        MappingReader.read(mojang, MappingSourceNsSwitch(renamer, "target"))
        val writer = MappingWriter.create(
            getIntermediary2Mojang2Yarn(),
            MappingFormat.TINY_2_FILE
        )
        val reorder = MappingDstNsReorder(writer, listOf("mojang", "yarn"))
        val switcher = MappingSourceNsSwitch(reorder, "intermediary")
        tree.accept(switcher)
    }

    private fun getOrDownloadMojangMappings(): Path? {
        val path = getMojang2OfficialMappingsPath()
        if (path.notExists()) {
            downloadMojangMappings()
            if (path.notExists()) {
                EssentialScripting.logger.error("Failed to load mojang mappings")
                return null
            }
        }
        return path
    }

    private fun downloadMojangMappings() {
        val url = getMojangMappingsUrl() ?: return
        try {
            NetworkingUtils.fetchAsStream(url) { reader ->
                getMojang2OfficialMappingsPath().outputStream().use { writer ->
                    reader.transferTo(writer)
                }
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

    private fun getOrDownloadYarnMappings(): Path? {
        val path = getOfficial2Intermediary2YarnMappingsPath()
        if (path.notExists()) {
            downloadYarnMappings()
            if (path.notExists()) {
                EssentialScripting.logger.error("Failed to load yarn mappings")
                return null
            }
        }
        return path
    }

    private fun downloadYarnMappings() {
        val url = getYarnMappingsUrl() ?: return
        try {
            NetworkingUtils.fetchAsStream(url) { stream ->
                GZIPInputStream(stream).use { gzip ->
                    getOfficial2Intermediary2YarnMappingsPath().outputStream().use { writer ->
                        gzip.transferTo(writer)
                    }
                }
            }
        } catch (e: IOException) {
            EssentialScripting.logger.error("Failed to download yarn mappings", e)
        }
    }

    private fun getYarnMappingsUrl(): String? {
        val meta = NetworkingUtils.fetchAsJsonArray("$YARN_META_URL/$version") ?: return null
        val entry = meta.maxByOrNull { entry ->
            entry.asJsonObject.get("build").asInt
        } ?: return null
        val version = entry.asJsonObject.get("version").asString
        return "$YARN_MAVEN_URL/$version/yarn-$version-tiny.gz"
    }

    private fun getIntermediary2Mojang2Yarn(): Path {
        return EssentialScriptingConfig.resolve("mappings")
            .resolve("intermediary2mojang2yarn")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getMojang2OfficialMappingsPath(): Path {
        return EssentialScriptingConfig.resolve("mappings")
            .resolve("mojang2official")
            .resolve("$version.txt")
            .createParentDirectories()
    }

    private fun getOfficial2Intermediary2YarnMappingsPath(): Path {
        return EssentialScriptingConfig.resolve("mappings")
            .resolve("official2intermediary2yarn")
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
}