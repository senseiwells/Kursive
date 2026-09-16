# Persistent Data

`context.stores` lets a script save data to disk so it survives being stopped
and restarted, and survives the game or server restarting. It's available on
both the client and the server and works the same way on each.

Data is written to disk when the script stops. On the server it's also written
whenever the world autosaves.

## Stores

A `PersistentDataStore` is a single JSON file of key/value pairs. There are two
kinds:

```kts
val private = context.stores.private("settings")
val shared = context.stores.shared("leaderboard")
```

- A **private** store belongs to your script. Two scripts asking for
  `private("settings")` each get their own file.
- A **shared** store is common to every script. Two scripts asking for
  `shared("leaderboard")` get the same file, so scripts can pass data between
  each other.

On the client, files live in `.minecraft/kursive/data`, and on the server in
`<world>/kursive/data`.

## Reading and Writing

Primitive values have dedicated helpers:

```kts
fun example(context: ScriptContext) {
    val store = context.stores.private("stats")

    val runs: Int = store.readIntOrDefault("runs", 0)
    store.storeInt("runs", runs + 1)

    store.storeString("last-player", "Steve")
    store.storeBoolean("enabled", true)
    store.remove("old-key")
}
```

Anything else goes through a `Codec`:

```kts
fun example(context: ScriptContext, player: Player) {
    store.store("home", BlockPos.CODEC, player.blockPosition())
    val home: BlockPos? = store.read("home", BlockPos.CODEC) 

    store.store("favourite", ItemStack.CODEC, player.mainHandItem)
}
```

You can read more about codecs in the
[fabric docs](https://docs.fabricmc.net/develop/serialization/codecs).

## Properties

For values, you read and write repeatedly, bind them to a Kotlin property with
`by`. Reads go straight to the store and writes are saved immediately:

```kts
fun example(context: ScriptContext) {
    val store = context.stores.private("settings")

    var enabled: Boolean by store.property("enabled", Codec.BOOL, true)
    var name: String? by store.property("name", Codec.STRING)

    if (enabled) {
        name = "senseiwells"
    }
}
```

## Reloading

If you edit the JSON file by hand while the script is running, call
`store.reload()` to pick up the changes.
