package me.senseiwells.kursive.client.gui.script

import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class NewScriptScreen(
    override val parent: ScriptsScreen
): ScriptNamingScreen(Component.literal("Create New Script")) {
    override fun description(): Component {
        return Component.literal("Creates a new .kts file in the scripts folder")
    }

    override fun doesScriptExist(name: String): Boolean {
        return this.parent.doesScriptExist(name)
    }

    override fun createConfirmButton(): Button {
        return Button.builder(Component.literal("Create")) {
            this.parent.createNewScript(this.getScriptNameWithoutSuffix())
            this.onClose()
        }.width(50).build()
    }
}