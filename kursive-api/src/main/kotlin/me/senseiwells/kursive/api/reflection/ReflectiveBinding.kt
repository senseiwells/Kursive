package me.senseiwells.kursive.api.reflection

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure

class ReflectiveBinding<T: Any, R> internal constructor() {
    private val lookup = MethodHandles.lookup()
    private val cache = ConcurrentHashMap<DelegateKey, Resolved>()

    operator fun getValue(thisRef: T, property: KProperty<*>): R {
        val returnType = property.returnType
        val classifier = returnType.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Unsupported property type: $returnType")

        val thisType = property.extensionReceiverParameter?.type?.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Property must have extension receiver")
        val key = DelegateKey(thisType, property.name, returnType.toString())

        val resolved = this.cache.getOrPut(key) {
            if (Function::class.java.isAssignableFrom(classifier.java)) {
                resolveMethod(thisType, property.name, returnType, classifier)
            } else {
                resolveField(thisType, property.name, returnType)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return when (resolved) {
            is Resolved.Method -> {
                val bound = resolved.handle.bindTo(thisRef)
                Proxy.newProxyInstance(
                    resolved.proxy.classLoader,
                    arrayOf(resolved.proxy)
                ) { _, method, args ->
                    if (method.name == "invoke") {
                        if (args == null) bound.invoke() else bound.invokeWithArguments(args.toList())
                    } else {
                        throw UnsupportedOperationException(method.name)
                    }
                } as R
            }
            is Resolved.Field -> resolved.handle.invoke(thisRef) as R
        }
    }

    private fun resolveMethod(
        owner: KClass<*>,
        name: String,
        returnType: kotlin.reflect.KType,
        classifier: KClass<*>,
    ): Resolved.Method {
        val typeArgs = returnType.arguments
        val paramTypes = typeArgs.dropLast(1).map { it.type }
        val lambdaReturnType = typeArgs.last().type

        val fn = owner.functions.firstOrNull { candidate ->
            candidate.name == name &&
                candidate.parameters.drop(1).map { it.type } == paramTypes &&
                candidate.returnType == lambdaReturnType
        }

        val javaMethod = fn?.javaMethod ?: run {
            val paramErasures = paramTypes.map { it?.jvmErasure?.java }
            val returnErasure = lambdaReturnType?.jvmErasure?.java
            this.findMethod(owner.java, name, paramErasures, returnErasure)
                ?: throw IllegalArgumentException("No method '$name' matching $returnType found on $owner")
        }
        javaMethod.isAccessible = true

        val handle = this.lookup.unreflect(javaMethod)
        return Resolved.Method(handle, classifier.java)
    }

    private fun findMethod(
        startClass: Class<*>,
        name: String,
        paramTypes: List<Class<*>?>,
        returnType: Class<*>?,
    ): Method? {
        var current: Class<*>? = startClass
        while (current != null) {
            val candidate = current.declaredMethods.firstOrNull { candidate ->
                candidate.name == name &&
                    candidate.parameterCount == paramTypes.size &&
                    candidate.parameterTypes.indices.all { i ->
                        paramTypes[i] == null || paramTypes[i]!!.isAssignableFrom(candidate.parameterTypes[i])
                    } && (returnType == null || returnType.isAssignableFrom(candidate.returnType))
            }
            if (candidate != null) {
                return candidate
            }
            current = current.superclass
        }
        return null
    }

    private fun resolveField(
        owner: KClass<*>,
        name: String,
        returnType: kotlin.reflect.KType,
    ): Resolved.Field {
        val prop = owner.memberProperties.firstOrNull { it.name == name && it.returnType == returnType }
        val getter = prop?.javaGetter?.also { it.isAccessible = true }

        val handle = if (getter == null) {
            val field = this.findField(owner.java, name)
                ?: throw IllegalArgumentException("No accessible getter or field for '$name' on $owner")
            field.isAccessible = true
            this.lookup.unreflectGetter(field)
        } else {
            this.lookup.unreflect(getter)
        }

        return Resolved.Field(handle)
    }

    private fun findField(startClass: Class<*>, name: String): Field? {
        var current: Class<*>? = startClass
        while (current != null) {
            val candidate = current.declaredFields.firstOrNull { it.name == name }
            if (candidate != null) {
                return candidate
            }
            current = current.superclass
        }
        return null
    }

    private data class DelegateKey(
        val owner: KClass<*>,
        val name: String,
        val signature: String,
    )

    private sealed class Resolved {
        class Method(val handle: MethodHandle, val proxy: Class<*>): Resolved()
        class Field(val handle: MethodHandle): Resolved()
    }
}