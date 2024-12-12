package me.senseiwells.essential_scripting.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.senseiwells.essential_scripting.EssentialScriptingConfig

object ModMenuEssentialClientImpl: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory(EssentialScriptingConfig::screen)
    }
}