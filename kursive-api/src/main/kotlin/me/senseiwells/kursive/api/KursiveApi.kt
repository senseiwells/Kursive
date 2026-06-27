package me.senseiwells.kursive.api

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.Version
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object KursiveApi: ModInitializer {
    const val MOD_ID = "kursive-api"

    val container: ModContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get()
    val version: Version = container.metadata.version

    override fun onInitialize() {

    }
}