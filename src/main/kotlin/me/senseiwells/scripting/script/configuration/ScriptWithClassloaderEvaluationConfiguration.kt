package me.senseiwells.scripting.script.configuration

import me.senseiwells.scripting.script.Script
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

class ScriptWithClassloaderEvaluationConfiguration: ScriptEvaluationConfiguration({
    jvm {
        baseClassLoader(Script::class.java.classLoader)
    }
})