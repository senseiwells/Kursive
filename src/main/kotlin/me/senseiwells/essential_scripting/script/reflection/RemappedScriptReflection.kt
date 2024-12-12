package me.senseiwells.essential_scripting.script.reflection

import me.senseiwells.essential_scripting.script.configuration.MappingType
import me.senseiwells.essential_scripting.utils.ScriptRemappingUtils
import me.senseiwells.scripting.api.ScriptReflection
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.lang.reflect.Field
import java.lang.reflect.Method

class RemappedScriptReflection(
    private val mappings: MemoryMappingTree,
    private val from: MappingType
): ScriptReflection {
    private val to = ScriptRemappingUtils.getCurrentMappings()

    private val fromIndex = this.mappings.dstNamespaces.indexOf(from.id)
    private val toIndex = this.mappings.dstNamespaces.indexOf(to.id)

    override fun getClass(name: String): Class<*> {
        val mapping = this.mappings.getClass(name.replace('.', '/'), this.fromIndex)
            ?: throw IllegalArgumentException("No such ${this.from.id} mapped class with name '$name'")
        val remapped = mapping.getName(this.toIndex)
            ?: throw IllegalStateException("Failed to remap class $name to ${this.to.id} mappings")
        return Class.forName(remapped.replace('/', '.'))
    }

    override fun getMethod(owner: Class<*>, name: String, vararg parameterTypes: Class<*>): Method {
        val remapped = this.getRemappedMethodName(owner, name, parameterTypes)
        return owner.getMethod(remapped, *parameterTypes)
    }

    override fun getDeclaredMethod(owner: Class<*>, name: String, vararg parameterTypes: Class<*>): Method {
        val remapped = this.getRemappedMethodName(owner, name, parameterTypes)
        return owner.getDeclaredMethod(remapped, *parameterTypes)
    }

    override fun getField(owner: Class<*>, name: String): Field {
        val remapped = this.getRemappedFieldName(owner, name)
        return owner.getField(remapped)
    }

    override fun getDeclaredField(owner: Class<*>, name: String): Field {
        val remapped = this.getRemappedFieldName(owner, name)
        return owner.getDeclaredField(remapped)
    }

    private fun getClassMapping(clazz: Class<*>): MappingTree.ClassMapping {
        return this.mappings.getClass(clazz.name.replace('.', '/'), this.toIndex)
            ?: throw IllegalArgumentException("No such ${this.to.id} mapped class with name '${clazz.name}'")
    }

    private fun getRemappedMethodName(
        owner: Class<*>,
        name: String,
        parameterTypes: Array<out Class<*>>
    ): String {
        val clazz = this.getClassMapping(owner)
        val desc = this.createMethodDescription(parameterTypes)
        val mapping = clazz.getMethod(name, desc, this.fromIndex) ?: throw IllegalArgumentException(
            "No such ${this.from.id} mapped method with name '$name' for class '${clazz.getName(this.fromIndex)}'"
        )
        return mapping.getName(this.toIndex) ?: throw IllegalStateException(
            "Failed to remap method '$name' for class '${clazz.getName(this.fromIndex)}' to ${this.to.id}"
        )
    }

    private fun getRemappedFieldName(owner: Class<*>, name: String): String {
        val clazz = this.getClassMapping(owner)
        val mapping = clazz.getField(name, null, this.fromIndex) ?: throw IllegalArgumentException(
            "No such ${this.from.id} mapped field with name '$name' for class '${clazz.getName(this.fromIndex)}'"
        )
        return mapping.getName(this.toIndex) ?: throw IllegalStateException(
            "Failed to remap field '$name' for class '${clazz.getName(this.fromIndex)}' to ${this.to.id}"
        )
    }

    private fun createMethodDescription(types: Array<out Class<*>>): String {
        val builder = StringBuilder()
        builder.append('(')
        for (type in types) {
            builder.append(type.encodedName())
        }
        builder.append(')')
        return builder.toString()
    }

    private fun Class<*>.encodedName(): String {
        if (this.isPrimitive) {
            return when (this) {
                Boolean::class.javaPrimitiveType -> "Z"
                Byte::class.javaPrimitiveType -> "B"
                Char::class.javaPrimitiveType -> "C"
                Double::class.javaPrimitiveType -> "D"
                Float::class.javaPrimitiveType -> "F"
                Int::class.javaPrimitiveType -> "I"
                Long::class.javaPrimitiveType -> "J"
                Short::class.javaPrimitiveType -> "S"
                Void::class.javaPrimitiveType -> "V"
                else -> throw MatchException("Primitive class ${this.simpleName} not matched!", null)
            }
        }
        if (this.isArray) {
            return this.name.replace('.', '/')
        }
        return "L${this.name.replace('.', '/')};"
    }
}