package me.senseiwells.kursive.client.gui.script.client

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import net.minecraft.client.Minecraft

class ClientScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ScriptsList(minecraft, width, height, y) {
    override fun refresh() {
        this.clearEntries()
        val environment = KursiveClient.environment(this.minecraft, listOf())
        for (script in KursiveClient.scripts.sortedBy { it.definition.name }) {
            this.addEntry(ScriptEntry(this, LocalScriptHandle(script, environment)))
        }
    }

    override fun dirty(): Boolean {
        return KursiveClient.scripts.dirty
    }
}