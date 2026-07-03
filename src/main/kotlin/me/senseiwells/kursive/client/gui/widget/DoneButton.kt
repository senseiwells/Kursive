package me.senseiwells.kursive.client.gui.widget

import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

@Suppress("FunctionName")
fun DoneButton(
    screen: Screen,
    name: Component = Component.literal("Done"),
    width: Int = 80
): Button {
    return Button.builder(name) {
        screen.onClose()
    }.size(width, 20).build()
}

@Suppress("FunctionName")
fun CancelButton(
    screen: Screen,
    name: Component = Component.literal("Cancel"),
    width: Int = 80
): Button {
    return DoneButton(screen, name, width)
}