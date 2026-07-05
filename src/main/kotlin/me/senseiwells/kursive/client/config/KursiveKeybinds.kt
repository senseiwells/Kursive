package me.senseiwells.kursive.client.config

import me.senseiwells.keybinds.api.InputKeys
import me.senseiwells.keybinds.api.Keybind
import me.senseiwells.keybinds.api.KeybindListener
import me.senseiwells.keybinds.api.KeybindManager
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.menu.KursiveMenuScreen
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.network.payload.serverbound.RestartRemoteScriptsPayload
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.yellow
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object KursiveKeybinds {
    private val KURSIVE_CATEGORY = KeyMapping.Category.register(kursive("general"))

    const val MENU_ID = "${Kursive.MOD_ID}:open_menu"
    const val RESTART_CLIENT_SCRIPTS_ID = "${Kursive.MOD_ID}:restart_client_scripts"
    const val RESTART_SERVER_SCRIPTS_ID = "${Kursive.MOD_ID}:restart_server_scripts"

    val menu: Keybind = this.register(MENU_ID, KursiveClientConfig.instance.menuKeys)
    val restartClientScripts: Keybind = this.register(RESTART_CLIENT_SCRIPTS_ID, KursiveClientConfig.instance.restartClientScriptsKeys)
    val restartServerScripts: Keybind = this.register(RESTART_SERVER_SCRIPTS_ID, KursiveClientConfig.instance.restartServerScriptsKeys)

    init {
        this.initialize()
    }

    internal fun load() {

    }

    private fun register(id: String, keys: InputKeys): Keybind {
        val keybind = KeybindManager.register(Identifier.parse(id), keys)
        KeybindManager.addToControlsScreen(KURSIVE_CATEGORY, keybind)
        return keybind
    }

    private fun initialize() {
        this.menu.addListener(KeybindListener.onPress {
            Minecraft.getInstance().gui.setScreen(KursiveMenuScreen())
        })

        this.restartClientScripts.addListener(KeybindListener.onPress {
            val minecraft = Minecraft.getInstance()
            val environment = KursiveClient.environment(Minecraft.getInstance())
            minecraft.gui.hud.chat.addClientSystemMessage(Component.literal("Restarting running client scripts").yellow())
            Kursive.restartScripts(environment, KursiveClient.scripts)
        })

        this.restartServerScripts.addListener(KeybindListener.onPress {
            if (ClientPlayNetworking.canSend(RestartRemoteScriptsPayload.TYPE)) {
                val minecraft = Minecraft.getInstance()
                minecraft.gui.hud.chat.addClientSystemMessage(Component.literal("Restarting running server scripts").yellow())
                ClientPlayNetworking.send(RestartRemoteScriptsPayload)
            }
        })
    }
}