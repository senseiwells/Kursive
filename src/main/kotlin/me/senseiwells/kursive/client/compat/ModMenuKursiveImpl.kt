package me.senseiwells.kursive.client.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.senseiwells.kursive.client.gui.menu.KursiveMenuScreen

object ModMenuKursiveImpl: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory(::KursiveMenuScreen)
    }
}