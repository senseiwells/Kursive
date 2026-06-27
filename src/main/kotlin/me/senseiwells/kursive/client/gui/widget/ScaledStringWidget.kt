package me.senseiwells.kursive.client.gui.widget

import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.network.chat.Component
import org.joml.Matrix3x2f

class ScaledStringWidget(
    component: Component,
    font: Font,
    var scale: Float = 1.0F
): StringWidget(component, font) {
    override fun getWidth(): Int {
        return (super.getWidth() * this.scale).toInt()
    }

    override fun getHeight(): Int {
        return (super.getHeight() * this.scale).toInt()
    }

    override fun visitLines(output: ActiveTextCollector) {
        val cx = this.x.toFloat()
        val cy = this.y.toFloat()
        val newPose = Matrix3x2f(output.defaultParameters().pose())
            .translate(cx, cy)
            .scale(this.scale)
            .translate(-cx, -cy)
        output.defaultParameters(output.defaultParameters().withPose(newPose))
        super.visitLines(output)
    }
}