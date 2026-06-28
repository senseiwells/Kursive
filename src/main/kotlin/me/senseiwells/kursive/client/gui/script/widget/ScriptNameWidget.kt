package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.utils.setTooltip
import net.casual.arcade.utils.component.Component
import net.casual.arcade.utils.component.plus
import net.casual.arcade.utils.component.teal
import net.casual.arcade.utils.component.yellow
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.network.chat.Component

class ScriptNameWidget(
    font: Font,
    private val script: ScriptHandle
): StringWidget(Component.literal("${script.name()}.kts"), font) {
    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val metadata = this.script.getMetadata()
        if (metadata != null) {
            this.setTooltip(Component {
                empty() + literal("Script Id: ") + literal(metadata.id).yellow() + nl +
                    literal("Version: ") + literal("${metadata.version}").teal()
            })
        }

        super.extractWidgetRenderState(graphics, mouseX, mouseY, a)
    }
}