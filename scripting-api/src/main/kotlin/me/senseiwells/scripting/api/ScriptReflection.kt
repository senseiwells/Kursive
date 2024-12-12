package me.senseiwells.scripting.api

import java.lang.reflect.Field
import java.lang.reflect.Method

interface ScriptReflection {
    fun getClass(name: String): Class<*>

    fun getMethod(owner: Class<*>, name: String, vararg parameterTypes: Class<*>): Method

    fun getDeclaredMethod(owner: Class<*>, name: String, vararg parameterTypes: Class<*>): Method

    fun getField(owner: Class<*>, name: String): Field

    fun getDeclaredField(owner: Class<*>, name: String): Field
}