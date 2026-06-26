package me.senseiwells.kursive.common.utils

import me.senseiwells.kursive.common.Kursive
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

object ScriptConfigurationUtils {
    val CLASSLOADER: ClassLoader = Kursive::class.java.classLoader

    val HOST_CONFIGURATION = ScriptingHostConfiguration {
        jvm {
            baseClassLoader(CLASSLOADER)
        }
    }
}