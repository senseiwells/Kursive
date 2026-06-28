package me.senseiwells.kursive.client.gui.widget

import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

@Suppress("FunctionName")
fun DoneButton(screen: Screen, width: Int = 80): Button {
    return Button.builder(Component.literal("Done")) {
        screen.onClose()
    }.size(width, 20).build()
}