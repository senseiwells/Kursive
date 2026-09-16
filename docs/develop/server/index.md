# Server-side Scripting

This section covers writing scripts that run on the server. If you're not sure
whether your script belongs on the server or the client, read
[Client vs Server](../client-vs-server.md) first.

## The Entrypoint

Every server script has a `main` function which Kursive calls when the script is
started. The full form takes the server and a context:

```kts
suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    awaitCancellation()
}
```

Our main function can be [suspending](https://kotlinlang.org/docs/coroutines-basics.html#suspending-functions).
This defines the lifetime of the script, if the `main` function ever returns then
our script is considered finished and will be stopped. In the example above we can see
that `awaitCancellation` is being called which prevents `main` from returning and instead
suspends execution until cancellation, i.e. until the script is manually stopped.

The `main` function is called when your script starts, which can be trigger from:
- Pressing "Start Script" in the server scripts gui.
- Running `/kursive-server start <name> [args...]` in-game.
- Automatically when the game starts, if the script's metadata has `auto = true`.

A script is stopped either when `main` returns, or is cancelled by:
- Pressing "Stop Script" in the server scripts gui.
- Running `kursive-server stop <name>`.
- Closing the game.

When a script stops anything registered via the script's `context` will be automatically
cleaned up for you, but anything else that needs cleaning up will need manual cleaning up
which can be done in a `finally` block:

```kotlin
suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    try {
        // Do thing that needs cleaning up later
        awaitCancellation()
    } finally {
        // Clean things up here!
    }
}
```

## The `MinecraftServer` Class

The instance of `MinecraftServer` that gets passed into your `main` entrypoint
allows you to interact with most things that exist on the server. In singleplayer
this'll be a `IntegratedServer` and on a dedicated server this'll be a `DedicatedServer`
instance. Some example members can be seen in the table below:

| Member               | Description                                                    |
|----------------------|----------------------------------------------------------------|
| `server.overworld()` | The overworld `ServerLevel`                                    |
| `server.nether()`    | The nether `ServerLevel` (this is an extension function)       |
| `server.end()`       | The end `ServerLevel` (this is an extension function)          |
| `server.players`     | Every connected `ServerPlayer` (this is an extension function) |

For example, sending a message to everyone online:

```kts
suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    server.players.broadcast(Component.literal("Hello World"))
}
```

## The `ServerScriptContext` Class

Kursive provides its own api for scripts. The provided `context` parameter allows
you to interact with it. These are designed to be safe and will be automatically
saved/cleaned up when your script is stopped.

| Member             | Description                                                         |
|--------------------|---------------------------------------------------------------------|
| `context.args`     | A `List<String>`; the arguments the script was started with         |
| `context.events`   | A `ListenerRegistry<ServerSideEvent>` to register event listeners   |
| `context.commands` | A `CommandRegistry<CommandSourceStack>` to register custom commands |
| `context.stores`   | A `PersistentDataStores` to store data over script runs             |

The following pages will touch on how to use the context members.