package me.senseiwells.kursive.api.reflection

@Suppress("Unused")
class ScriptReflection {
    private val binding = ReflectiveBinding<Any, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T: Any, R> binding(): ReflectiveBinding<T, R> {
        return this.binding as ReflectiveBinding<T, R>
    }
}