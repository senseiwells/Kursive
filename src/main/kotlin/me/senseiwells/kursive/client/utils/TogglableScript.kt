package me.senseiwells.kursive.client.utils

import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import net.minecraft.client.Minecraft

interface TogglableScript {
    fun isRunning(): Boolean

    fun start()

    fun stop()

    fun toggle() {
        if (this.isRunning()) {
            this.stop()
        } else {
            this.start()
        }
    }

    companion object {
        fun from(instance: ScriptInstance<Minecraft>, environment: ExecutionEnvironment<Minecraft, *>): TogglableScript {
            return object: TogglableScript {
                override fun isRunning(): Boolean {
                    return instance.isRunning()
                }

                override fun start() {
                    environment.launch {
                        val result = instance.start(environment)
                        Kursive.logDiagnostics(result)
                    }
                }

                override fun stop() {
                    instance.stop()
                }
            }
        }
    }
}