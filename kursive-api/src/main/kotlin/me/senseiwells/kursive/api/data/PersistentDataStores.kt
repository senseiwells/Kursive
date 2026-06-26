package me.senseiwells.kursive.api.data

interface PersistentDataStores {
    fun shared(id: String): PersistentDataStore

    fun private(id: String): PersistentDataStore
}