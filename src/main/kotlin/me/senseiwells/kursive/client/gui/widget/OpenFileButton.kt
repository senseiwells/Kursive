package me.senseiwells.kursive.client.gui.widget

import com.mojang.blaze3d.Blaze3D
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.network.chat.Component
import java.nio.file.Path

@Suppress("FunctionName")
fun OpenFileButton(message: Component, path: () -> Path): Button {
    return SpriteIconButton.builder(message, {
        Blaze3D.openPath(path.invoke())
    }, true).width(20).sprite(kursive("icon/open_file"), 16, 16).withTootip().build()
}