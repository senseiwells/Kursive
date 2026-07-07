package me.senseiwells.kursive.client.utils

import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component

fun AbstractWidget.setTooltip(tooltip: Component) {
    this.setTooltip(Tooltip.create(tooltip))
}

fun AbstractWidget.setActiveAndTooltip(active: Boolean, tooltip: Component?) {
    this.active = active
    if (tooltip != null) {
        this.setTooltip(tooltip)
    } else {
        this.setTooltip(null)
    }
}