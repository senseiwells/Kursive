package me.senseiwells.kursive.client.gui.scripts

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ContainerObjectSelectionList

abstract class ScriptsList<E: ScriptsList.Entry<E>>(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    itemHeight: Int
): ContainerObjectSelectionList<E>(minecraft, width, height, y, itemHeight) {
    init {
        this.refresh()
    }

    abstract fun refresh()

    fun tick() {
        if (this.dirty()) {
            this.refresh()
        }
    }

    override fun getRowWidth(): Int {
        return 280
    }

    protected abstract fun dirty(): Boolean

    abstract class Entry<E: Entry<E>>: ContainerObjectSelectionList.Entry<E>()
}