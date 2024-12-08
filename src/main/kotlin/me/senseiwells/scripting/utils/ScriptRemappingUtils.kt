package me.senseiwells.scripting.utils

import com.google.gson.JsonObject
import kotlinx.io.IOException
import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.EssentialScriptingConfig
import me.senseiwells.scripting.script.configuration.MappingType
import net.fabricmc.loader.api.FabricLoader
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
import java.nio.file.Path
import java.util.EnumMap
import java.util.zip.GZIPInputStream
import kotlin.io.path.*

object ScriptRemappingUtils {
    private const val MOJANG_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    private const val YARN_META_URL = "https://meta.fabricmc.net/v2/versions/yarn"
    private const val YARN_MAVEN_URL = "https://maven.fabricmc.net/net/fabricmc/yarn"

    private val version = SharedConstants.getCurrentVersion().name
    private val mappings by lazy(ScriptRemappingUtils::createMappingTree)

    private var mappedJars = EnumMap<_, Path>(MappingType::class.java)

    fun getMappings(from: MappingType, to: MappingType = this.getCurrentMappings()): IMappingProvider {
        return this.mappings.provider(from.id, to.id, false)
    }

    fun getMappedJar(type: MappingType): Path? {
        return this.mappedJars[type]
    }

    fun shouldRemap(type: MappingType): Boolean {
        return this.getCurrentMappings() != type
    }

    internal fun load() {
        Util.ioPool().execute {
            this.writeIntermediary2Mojang2YarnMappings()
            this.createMappedJar(MappingType.Yarn)
            this.createMappedJar(MappingType.Mojang)
        }
    }

    private fun isCurrentIntermediary(): Boolean {
        return !FabricLoader.getInstance().isDevelopmentEnvironment
    }

    private fun getCurrentMappings(): MappingType {
        return if (this.isCurrentIntermediary()) MappingType.Intermediary else MappingType.Mojang
    }

    private fun createMappedJar(to: MappingType) {
        val from = this.getCurrentMappings()

        val originalJar = Path.of(MinecraftServer::class.java.protectionDomain.codeSource.location.toURI())
        if (from.id == to.id) {
            this.mappedJars[to] = originalJar
            return
        }

        val intermediaryJarName = originalJar.nameWithoutExtension
        val mappedJar = originalJar.resolveSibling("${intermediaryJarName}-mapped-${to.id}.jar")
        if (mappedJar.exists()) {
            this.mappedJars[to] = mappedJar
            return
        }
        val remapper = TinyRemapper.newRemapper()
            .withMappings(this.mappings.provider(from.id, to.id, true))
            .build()
        try {
            OutputConsumerPath.Builder(mappedJar).build().use { consumer ->
                consumer.addNonClassFiles(originalJar, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(originalJar)
                remapper.apply(consumer)
            }
            this.mappedJars[to] = mappedJar
        } finally {
            remapper.finish()
        }
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
        val meta = NetworkingUtils.fetchAsJsonArray("${YARN_META_URL}/$version") ?: return null
        val entry = meta.maxByOrNull { entry ->
            entry.asJsonObject.get("build").asInt
        } ?: return null
        val version = entry.asJsonObject.get("version").asString
        return "${YARN_MAVEN_URL}/$version/yarn-$version-tiny.gz"
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