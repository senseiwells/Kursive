package me.senseiwells.kursive.common.script.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.serialization.Codec
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import me.senseiwells.kursive.api.data.PersistentDataStore
import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.serialization.createSerializationContext
import net.casual.arcade.utils.serialization.json.JsonValueInput
import net.casual.arcade.utils.serialization.json.JsonValueOutput
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.util.ProblemReporter
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull

class FileBasedDataStore(
    val path: Path,
    private val lookup: () -> HolderLookup.Provider?
): PersistentDataStore {
    private val cache = Object2ObjectLinkedOpenHashMap<String, ValueWithCodec<*>?>()
    private lateinit var raw: JsonObject

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalStdlibApi::class)
    override fun <T: Any> read(key: String, codec: Codec<T>): T? {
        this.lazyInit()

        return this.cache.getOrPutIfMissing(key) pim@ {
            val lookup = this.lookup.invoke() ?: RegistryAccess.EMPTY
            val input = JsonValueInput.create(ProblemReporter.DISCARDING, lookup, this.raw)
            val value = input.read(key, codec).getOrNull() ?: return@pim null
            ValueWithCodec(value, codec)
        }?.value as T?
    }

    override fun readBooleanOrDefault(key: String, fallback: Boolean): Boolean {
        return this.readPrimitive(key, Codec.BOOL, fallback, JsonPrimitive::isBoolean, JsonPrimitive::getAsBoolean)
    }

    override fun readByteOrDefault(key: String, fallback: Byte): Byte {
        return this.readPrimitive(key, Codec.BYTE, fallback, JsonPrimitive::isNumber, JsonPrimitive::getAsByte)
    }

    override fun readIntOrDefault(key: String, fallback: Int): Int {
        return this.readPrimitive(key, Codec.INT, fallback, JsonPrimitive::isNumber, JsonPrimitive::getAsInt)
    }

    override fun readLongOrDefault(key: String, fallback: Long): Long {
        return this.readPrimitive(key, Codec.LONG, fallback, JsonPrimitive::isNumber, JsonPrimitive::getAsLong)
    }

    override fun readFloatOrDefault(key: String, fallback: Float): Float {
        return this.readPrimitive(key, Codec.FLOAT, fallback, JsonPrimitive::isNumber, JsonPrimitive::getAsFloat)
    }

    override fun readDoubleOrDefault(key: String, fallback: Double): Double {
        return this.readPrimitive(key, Codec.DOUBLE, fallback, JsonPrimitive::isNumber, JsonPrimitive::getAsDouble)
    }

    override fun readStringOrDefault(key: String, fallback: String): String {
        return this.readPrimitive(key, Codec.STRING, fallback, JsonPrimitive::isString, JsonPrimitive::getAsString)
    }

    override fun <T: Any> store(key: String, codec: Codec<T>, value: T) {
        this.lazyInit()

        this.cache[key] = ValueWithCodec(value, codec)
    }

    override fun remove(key: String) {
        this.lazyInit()

        this.raw.remove(key)
        this.cache.remove(key)
    }

    override fun reload() {
        this.cache.clear()

        if (!this.path.isRegularFile()) {
            this.path.createParentDirectories().writeText("{}")
            this.raw = JsonObject()
            return
        }
        this.raw = JsonUtils.decodeRaw<JsonObject>(this.path)
    }

    fun write() {
        val output = JsonValueOutput.create(ProblemReporter.DISCARDING, this.ops(), this.raw.deepCopy())

        fun <T: Any> store(key: String, vwc: ValueWithCodec<T>) = output.store(key, vwc.codec, vwc.value)

        for ((key, vwc) in this.cache) {
            if (vwc != null) {
                store(key, vwc)
            }
        }

        JsonUtils.encodeRaw(output.buildResult(), this.path.createParentDirectories())
    }

    private fun ops(): DynamicOps<JsonElement> {
        return this.lookup.invoke().createSerializationContext(JsonOps.INSTANCE)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private inline fun <reified T: Any> readPrimitive(
        key: String,
        codec: Codec<T>,
        fallback: T,
        crossinline check: (JsonPrimitive) -> Boolean,
        crossinline cast: (JsonPrimitive) -> T
    ): T {
        this.lazyInit()
        return this.cache.getOrPutIfMissing(key) pim@ {
            val element = this.raw.get(key) as? JsonPrimitive ?: return@pim null
            if (check.invoke(element)) ValueWithCodec(cast.invoke(element), codec) else null
        }?.value as T? ?: fallback
    }

    private fun lazyInit() {
        if (!this::raw.isInitialized) {
            this.reload()
        }
    }

    private data class ValueWithCodec<T: Any>(val value: T, val codec: Codec<T>)
}