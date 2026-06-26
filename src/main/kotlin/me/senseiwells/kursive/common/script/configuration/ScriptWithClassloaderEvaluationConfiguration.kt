package me.senseiwells.kursive.common.script.configuration

import me.senseiwells.kursive.common.utils.ScriptConfigurationUtils
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

object ScriptWithClassloaderEvaluationConfiguration: ScriptEvaluationConfiguration({
    jvm {
        baseClassLoader(ScriptConfigurationUtils.CLASSLOADER)
    }
}) {
    private fun readResolve(): Any = ScriptWithClassloaderEvaluationConfiguration
}