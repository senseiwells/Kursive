package me.senseiwells.kursive.client.script.keybinds

import me.senseiwells.keybinds.api.InputKeys
import me.senseiwells.keybinds.api.Keybind
import me.senseiwells.keybinds.api.KeybindManager
import me.senseiwells.keybinds.api.SimpleKeybind
import me.senseiwells.kursive.api.keybind.KeybindRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class ScriptKeybindManager: KeybindRegistry {
    private val keybinds = HashMap<Identifier, Keybind>()

    override fun register(id: Identifier, name: Component, keys: InputKeys): Keybind {
        val keybind = SimpleKeybind(name, keys)
        this.keybinds[id] = keybind
        KeybindManager.register(id, keybind)
        return keybind
    }

    override fun listInControlsScreen(category: KeyMapping.Category, keybind: Keybind) {
        KeybindManager.addToControlsScreen(category, keybind)
    }

    internal fun unregister() {
        for (keybind in this.keybinds.keys) {
            KeybindManager.unregister(keybind)
        }
    }
}