package me.senseiwells.kursive.client.gui.menu

import me.senseiwells.kursive.client.gui.menu.widget.MenuCardWidget
import me.senseiwells.kursive.client.gui.script.client.ClientScriptsScreen
import me.senseiwells.kursive.client.gui.script.server.ServerScriptsScreen
import me.senseiwells.kursive.client.gui.widget.DoneButton
import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget
import me.senseiwells.kursive.client.utils.setTooltip
import me.senseiwells.kursive.common.network.payload.serverbound.RequestRemoteScriptsPayload
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class KursiveMenuScreen(
    private val parent: Screen?
): Screen(Component.literal("Kursive Menu")) {
    private val layout = HeaderAndFooterLayout(this)

    private lateinit var server: MenuCardWidget

    override fun init() {
        this.layout.addToHeader(ScaledStringWidget(this.title, this.font, 1.5F)) { settings ->
            settings.paddingTop(16)
        }

        this.layout.addToFooter(DoneButton(this, 120))

        this.layout.addToContents(this.createCardGrid())

        this.layout.visitWidgets(this::addRenderableWidget)
        this.repositionElements()
    }

    override fun tick() {
        if (ClientPlayNetworking.canSend(RequestRemoteScriptsPayload.TYPE)) {
            this.server.active = true
            this.server.setTooltip(null)
        } else {
            this.server.active = false
            this.server.setTooltip(Component {
                empty() + literal("Unavailable").red() + nl +
                    literal("You must be connected to a server running Kursive to run server scripts")
            })
        }
    }

    override fun repositionElements() {
        this.layout.arrangeElements()
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }

    private fun createCardGrid(): GridLayout {
        val layout = GridLayout()

        val client = MenuCardWidget.builder(Component.literal("Kursive Client Scripts").bold(), kursive("icon/client"))
            .description(Component.literal("Run scripts on your client").silver())
            .action { this.minecraft.gui.setScreen(ClientScriptsScreen(this)) }
            .build()
        layout.addChild(client, 0, 0)

        this.server = MenuCardWidget.builder(Component.literal("Kursive Server Scripts").bold(), kursive("icon/server"))
            .description(Component.literal("Run scripts on the server").silver())
            .action { this.minecraft.gui.setScreen(ServerScriptsScreen(this)) }
            .build()
        layout.addChild(this.server, 1, 0) { settings ->
            settings.paddingTop(10)
        }

        return layout
    }
}