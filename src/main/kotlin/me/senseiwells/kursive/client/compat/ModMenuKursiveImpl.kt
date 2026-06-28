package me.senseiwells.kursive.client.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.senseiwells.kursive.client.gui.script.client.ClientScriptsScreen

object ModMenuKursiveImpl: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory(::ClientScriptsScreen)
    }
}