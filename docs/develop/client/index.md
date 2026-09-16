# Client-side Scripting

This section covers writing scripts that run on the client. If you're not sure
whether your script belongs on the client or the server, read
[Client vs Server](../client-vs-server.md) first.

## The Entrypoint

Every client script has a `main` function which Kursive calls when the script is
started. The full form takes the client and a context:

```kts
suspend fun main(minecraft: Minecraft, context: ClientScriptContext) {
    awaitCancellation()
}
```
Our main function can be [suspending](https://kotlinlang.org/docs/coroutines-basics.html#suspending-functions).
This defines the lifetime of the script, if the `main` function ever returns then
our script is considered finished and will be stopped. In the example above we can see
that `awaitCancellation` is being called which prevents `main` from returning and instead
suspends execution until cancellation, i.e. until the script is manually stopped.

The `main` function is called when your script starts, which can be trigger from:
- Pressing "Start Script" in the client scripts gui.
- Running `/kursive-client start <name> [args...]` in-game.
- Automatically when the game starts, if the script's metadata has `auto = true`.

A script is stopped either when `main` returns, or is cancelled by:
- Pressing "Stop Script" in the client scripts gui.
- Running `kursive-client stop <name>`.
- Closing the game.

When a script stops anything registered via the script's `context` will be automatically
cleaned up for you, but anything else that needs cleaning up will need manual cleaning up
which can be done in a `finally` block:

```kotlin
suspend fun main(minecraft: Minecraft, context: ClientScriptContext) {
    try {
        // Do thing that needs cleaning up later
        awaitCancellation()
    } finally {
        // Clean things up here!
    }
}
```

## The `Minecraft` class

The instance of `Minecraft` that gets passed into your `main` entrypoint allows you
to interact with most aspects of the running game client. Some example members can be seen 
in the table below:

| Member                 | Description                                                     |
|------------------------|-----------------------------------------------------------------|
| `minecraft.player`     | Your `LocalPlayer`, or `null` if you're not in a world          |
| `minecraft.connection` | The connection to the server, or `null` if not connected        |
| `minecraft.level`      | The `ClientLevel` you're in, or `null` if you're not in a world |
| `minecraft.gui`        | The in-game GUI                                                 |

For example, sending a message to your own chat:

```kts
suspend fun main(minecraft: Minecraft, context: ClientScriptContext) {
    // Will only send if you're in-game (minecraft.player != null)
    minecraft.player?.sendSystemMessage(Component.literal("Hello from a script!"))
}
```

Keep in mind that `minecraft.level` is only the client's local copy of the world,
you'll be able to get blocks/entities within your player's view distance, although
not all entity data/block entity data will be present, and directly modifying the 
world here will have no effect on the actual server-side world.

## The `ClientScriptContext` Class

Kursive provides its own api for scripts. The provided `context` parameter allows
you to interact with it. These are designed to be safe and will be automatically
saved/cleaned up when your script is stopped.

| Member             | Description                                                       |
|--------------------|-------------------------------------------------------------------|
| `context.args`     | A `List<String>`; the arguments the script was started with       |
| `context.events`   | A `ListenerRegistry<ClientSideEvent>` to register event listeners |
| `context.keybinds` | A `KeybindRegistry` to register keybinds                          |
| `context.stores`   | A `PersistentDataStores` to store data over script runs           |

The following pages will touch on how to use the context members.