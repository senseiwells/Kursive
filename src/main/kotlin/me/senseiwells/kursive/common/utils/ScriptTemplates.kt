package me.senseiwells.kursive.common.utils

import me.senseiwells.kursive.annotation.KursiveScript
import me.senseiwells.kursive.api.KursiveApi
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import net.casual.arcade.utils.string.CamelCase

object ScriptTemplates {
    fun create(
        minecraftType: Class<*>,
        contextType: Class<*>,
        imports: List<String> = listOf(),
        body: String = "// TODO: Write your code here!",
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

        val minecraftName = CamelCase.decode(minecraftType.simpleName).last()

        val builder = StringBuilder()
        if (metadata != null) {
            builder.append("""@file:KursiveScript(id = "${metadata.id}", version = "${metadata.version}")""").append('\n')
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