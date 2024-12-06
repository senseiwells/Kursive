package me.senseiwells.scripting.utils

import com.google.gson.JsonObject
import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.EssentialScriptingConfig
import net.fabricmc.loom.util.TinyRemapperHelper
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.adapter.MappingNsRenamer
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.tinyremapper.IMappingProvider
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.util.GsonHelper
import java.nio.file.Path
import kotlin.io.path.*

object ScriptRemappingUtils {
    private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private const val INTERMEDIARY_URL = "https://raw.githubusercontent.com/FabricMC/intermediary/refs/heads/master/mappings"

    private val version = SharedConstants.getCurrentVersion().name
    private val mappings by lazy(ScriptRemappingUtils::createMappingTree)

    private var mojangJar: Path? = null

    fun getMojang2IntermediaryMappings(): IMappingProvider {
        return TinyRemapperHelper.create(this.mappings, "mojang", "intermediary", false)
    }

    fun getIntermediary2MojangMappings(): IMappingProvider {
        return TinyRemapperHelper.create(this.mappings, "intermediary", "mojang", false)
    }

    fun getMojangClientJar(): Path? {
        return this.mojangJar
    }

    internal fun load() {
        Util.ioPool().execute {
            this.writeMojang2IntermediaryMappings()
            this.createMojangClientJar()
        }
    }

    private fun createMojangClientJar() {
        val intermediaryJar = Path.of(Minecraft::class.java.protectionDomain.codeSource.location.toURI())
        val mojangJar = intermediaryJar.resolveSibling("client-mojang.jar")
        if (mojangJar.exists()) {
            this.mojangJar = mojangJar
            return
        }
        val remapper = TinyRemapper.newRemapper()
            .withMappings(this.getIntermediary2MojangMappings())
            .build()
        try {
            OutputConsumerPath.Builder(mojangJar).build().use { consumer ->
                consumer.addNonClassFiles(intermediaryJar, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(intermediaryJar)
                remapper.apply(consumer)
            }
            this.mojangJar = mojangJar
        } finally {
            remapper.finish()
        }
    }

    private fun createMappingTree(): MemoryMappingTree {
        val tree = MemoryMappingTree()
        val path = getMojang2IntermediaryMappingsPath()
        if (path.notExists()) {
            writeMojang2IntermediaryMappings()
        }
        if (path.isReadable()) {
            MappingReader.read(path, tree)
        }
        return tree
    }

    private fun writeMojang2IntermediaryMappings() {
        val renames = mapOf("source" to "mojang", "target" to "official")
        val tree = MemoryMappingTree()
        val renamer = MappingNsRenamer(tree, renames)
        val intermediary = getOrDownloadIntermediaryMappings()
        val mojang = getOrDownloadMojangMappings()
        if (mojang == null || intermediary == null) {
            return
        }
        MappingReader.read(mojang, renamer)
        MappingReader.read(intermediary, renamer)
        val writer = MappingWriter.create(
            getMojang2IntermediaryMappingsPath(),
            MappingFormat.TINY_2_FILE
        )
        tree.dstNamespaces = listOf("intermediary")
        tree.accept(writer)
    }

    private fun getOrDownloadIntermediaryMappings(): Path? {
        val path = getOfficial2IntermediaryMappingsPath()
        if (path.notExists()) {
            downloadIntermediaryMappings()
            if (path.notExists()) {
                EssentialScripting.logger.error("Failed to load intermediary mappings")
                return null
            }
        }
        return path
    }

    private fun downloadIntermediaryMappings() {
        NetworkingUtils.fetch("$INTERMEDIARY_URL/$version.tiny") { reader ->
            getOfficial2IntermediaryMappingsPath().writer().use { writer ->
                reader.transferTo(writer)
            }
        }
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
        NetworkingUtils.fetch(url) { reader ->
            getMojang2OfficialMappingsPath().writer().use { writer ->
                reader.transferTo(writer)
            }
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
        val manifest = NetworkingUtils.fetchAsJsonObject(MANIFEST_URL) ?: return null
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

    private fun getMojang2IntermediaryMappingsPath(): Path {
        return EssentialScriptingConfig.resolve("mappings")
            .resolve("mojang2intermediary")
            .resolve("$version.tiny")
            .createParentDirectories()
    }

    private fun getMojang2OfficialMappingsPath(): Path {
        return EssentialScriptingConfig.resolve("mappings")
            .resolve("mojang2official")
            .resolve("$version.txt")
            .createParentDirectories()
    }

    private fun getOfficial2IntermediaryMappingsPath(): Path {
        return EssentialScriptingConfig.resolve("mappings")
            .resolve("official2intermediary")
            .resolve("$version.tiny")
            .createParentDirectories()
    }
}