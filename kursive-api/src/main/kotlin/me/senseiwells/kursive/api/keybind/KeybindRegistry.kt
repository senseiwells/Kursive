package me.senseiwells.kursive.api.keybind

import me.senseiwells.keybinds.api.InputKeys
import me.senseiwells.keybinds.api.Keybind
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.jetbrains.annotations.ApiStatus.NonExtendable

interface KeybindRegistry {
    fun register(id: Identifier, name: Component, keys: InputKeys): Keybind

    @NonExtendable
    fun register(id: Identifier, name: Component, vararg keys: Int): Keybind {
        return this.register(id, name, InputKeys.of(*keys))
    }

    fun listInControlsScreen(category: KeyMapping.Category, keybind: Keybind)
}