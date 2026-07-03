package me.senseiwells.kursive.client.gui.script.server

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class UploadScriptsScreen(
    private val parent: ServerScriptsScreen
): Screen(Component.literal("Upload Server Scripts")) {
    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }
}