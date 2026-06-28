package me.senseiwells.kursive.client.script.executable

import me.senseiwells.kursive.common.network.data.RemoteScriptData
import me.senseiwells.kursive.common.network.payload.serverbound.CompileRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.StartRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.StopRemoteScriptPayload
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.diagnostics.FormattedDiagnostics
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class RemoteScriptHandle(
    private val id: ScriptInstance.Id,
    private var name: String,
    private var running: Boolean,
    private var compiled: Boolean,
    private var metadata: ScriptMetadata?,
    private var diagnostics: FormattedDiagnostics?,
    private val sender: PayloadSender
): ScriptHandle {
    constructor(
        id: ScriptInstance.Id,
        data: RemoteScriptData,
        sender: PayloadSender
    ): this(id, data.name, data.running, data.compiled, data.metadata, data.diagnostics, sender)

    fun update(data: RemoteScriptData) {
        this.name = data.name
        this.running = data.running
        this.compiled = data.compiled
        this.metadata = data.metadata
        this.diagnostics = data.diagnostics
    }

    override fun id(): ScriptInstance.Id {
        return this.id
    }

    override fun name(): String {
        return this.name
    }

    override fun isRunning(): Boolean {
        return this.running
    }

    override fun isCompiled(): Boolean {
        return this.compiled
    }

    override fun getMetadata(): ScriptMetadata? {
        return this.metadata
    }

    override fun getDiagnostics(): FormattedDiagnostics? {
        return this.diagnostics
    }

    override fun start() {
        this.sender.send(StartRemoteScriptPayload(this.id))
    }

    override fun compile() {
        this.sender.send(CompileRemoteScriptPayload(this.id))
    }

    override fun stop() {
        this.sender.send(StopRemoteScriptPayload(this.id))
    }

    fun interface PayloadSender {
        fun send(payload: CustomPacketPayload)
    }
}