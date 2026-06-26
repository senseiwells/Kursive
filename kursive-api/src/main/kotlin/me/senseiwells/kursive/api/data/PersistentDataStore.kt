package me.senseiwells.kursive.api.data

import com.mojang.serialization.Codec
import kotlin.reflect.KProperty

interface PersistentDataStore {
    fun <T: Any> read(key: String, codec: Codec<T>): T?

    fun readBooleanOrDefault(key: String, fallback: Boolean): Boolean {
        return this.read(key, Codec.BOOL) ?: fallback
    }

    fun readByteOrDefault(key: String, fallback: Byte): Byte {
        return this.read(key, Codec.BYTE) ?: fallback
    }

    fun readIntOrDefault(key: String, fallback: Int): Int {
        return this.read(key, Codec.INT) ?: fallback
    }

    fun readLongOrDefault(key: String, fallback: Long): Long {
        return this.read(key, Codec.LONG) ?: fallback
    }

    fun readFloatOrDefault(key: String, fallback: Float): Float {
        return this.read(key, Codec.FLOAT) ?: fallback
    }

    fun readDoubleOrDefault(key: String, fallback: Double): Double {
        return this.read(key, Codec.DOUBLE) ?: fallback
    }

    fun readStringOrDefault(key: String, fallback: String): String {
        return this.read(key, Codec.STRING) ?: fallback
    }

    fun <T: Any> store(key: String, codec: Codec<T>, value: T)

    fun storeBoolean(key: String, value: Boolean) {
        this.store(key, Codec.BOOL, value)
    }

    fun storeByte(key: String, value: Byte) {
        this.store(key, Codec.BYTE, value)
    }

    fun storeInt(key: String, value: Int) {
        this.store(key, Codec.INT, value)
    }

    fun storeLong(key: String, value: Long) {
        this.store(key, Codec.LONG, value)
    }

    fun storeFloat(key: String, value: Float) {
        this.store(key, Codec.FLOAT, value)
    }

    fun storeDouble(key: String, value: Double) {
        this.store(key, Codec.DOUBLE, value)
    }

    fun storeString(key: String, value: String) {
        this.store(key, Codec.STRING, value)
    }

    fun remove(key: String)

    fun reload()

    fun <T> property(key: String, codec: Codec<T & Any>, fallback: T): Property<T> {
        return Property(this, key, codec, fallback)
    }

    fun <T: Any> property(key: String, codec: Codec<T>): Property<T?> {
        return this.property(key, codec, null)
    }

    class Property<T> internal constructor(
        private val store: PersistentDataStore,
        private val key: String,
        private val codec: Codec<T & Any>,
        private val fallback: T
    ) {
        fun get(): T {
            return this.store.read(this.key, this.codec) ?: this.fallback
        }

        fun set(value: T) {
            if (value == null) {
                this.store.remove(this.key)
            } else {
                this.store.store(this.key, this.codec, value)
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return this.get()
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.set(value)
        }
    }
}