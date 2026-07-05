package me.senseiwells.kursive.common.utils

import kotlinx.io.IOException
import me.senseiwells.kursive.annotation.KursiveScript
import me.senseiwells.kursive.api.KursiveApi
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import net.casual.arcade.utils.string.CamelCase
import java.nio.file.Path
import kotlin.io.path.writeText

object ScriptTemplates {
    private const val DEFAULT_BODY = "// TODO: Write your code here!"

    fun write(
        path: Path,
        minecraftType: Class<*>,
        contextType: Class<*>,
        imports: List<String> = listOf(),
        body: String = DEFAULT_BODY,
        metadata: ScriptMetadata? = null
    ) {
        val template = this.create(minecraftType, contextType, imports, body, metadata)
        try {
            path.writeText(template)
        } catch (e: IOException) {
            Kursive.logger.error("Failed to create script", e)
        }
    }

    fun create(
        minecraftType: Class<*>,
        contextType: Class<*>,
        imports: List<String> = listOf(),
        body: String = DEFAULT_BODY,
        metadata: ScriptMetadata? = null
    ): String {
        val imports = mutableListOf(
            minecraftType.canonicalName,
            contextType.canonicalName,
            *imports.toTypedArray()
        )
        if (metadata != null) {
            imports += KursiveScript::class.java.canonicalName
        }

        val minecraftName = CamelCase.decode(minecraftType.simpleName).last().lowercase()

        val builder = StringBuilder()
        if (metadata != null) {
            metadata.minecraftVersion.toString()
            builder.append("""@file:KursiveScript(id = "${metadata.id}", version = "${metadata.version}", auto = ${metadata.auto}, type = "${metadata.type.name.lowercase()}", minecraft = "${metadata.minecraftVersion}")""")
            builder.append('\n')
        }
        builder.append("""@file:DependsOn("me.senseiwells:kmc:${KursiveApi.version}")""").append('\n')
        builder.append('\n')
        builder.append(imports.sorted().joinToString("\n") { import -> "import $import" }).append('\n')
        builder.append('\n')
        builder.append("suspend fun main($minecraftName: ${minecraftType.simpleName}, context: ${contextType.simpleName}) {\n")
        builder.append("    $body\n")
        builder.append("}")
        return builder.toString()
    }
}