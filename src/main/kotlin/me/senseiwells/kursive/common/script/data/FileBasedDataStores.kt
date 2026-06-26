package me.senseiwells.kursive.common.script.data

import me.senseiwells.kursive.api.data.PersistentDataStore
import me.senseiwells.kursive.api.data.PersistentDataStores
import net.minecraft.core.HolderLookup
import java.nio.file.Path

class FileBasedDataStores(
    private val path: Path,
    private val id: String,
    private val lookup: () -> HolderLookup.Provider?
): PersistentDataStores {
    private val shared = HashMap<String, FileBasedDataStore>()
    private val private = HashMap<String, FileBasedDataStore>()

    override fun shared(id: String): PersistentDataStore {
        return this.shared.getOrPut(id) {
            FileBasedDataStore(this.path.resolve("shared").resolve("$id.json"), this.lookup)
        }
    }

    override fun private(id: String): PersistentDataStore {
        return this.private.getOrPut(id) {
            FileBasedDataStore(this.path.resolve("private").resolve(this.id).resolve("$id.json"), this.lookup)
        }
    }

    fun write() {
        for (store in this.shared.values + this.private.values) {
            store.write()
        }
    }
}