package me.senseiwells.scripting.script.configuration

import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

object ScriptWithClassloaderEvaluationConfiguration: ScriptEvaluationConfiguration({
    jvm {
        baseClassLoader(Script::class.java.classLoader)
    }
}) {
    private fun readResolve(): Any = ScriptWithClassloaderEvaluationConfiguration
}