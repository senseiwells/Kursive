# Server Events

Being able to listen to events is a fundamental feature. Kursive uses
[Arcade](https://github.com/CasualChampionships/arcade)'s event system and is
provided through `context.events`. All server events live under
`net.casual.arcade.events.server` and implement `ServerSideEvent`.

While typically fabric mods use events provided by fabric api for example:
`ServerTickEvents.START_SERVER_TICK`, these should be avoided as registered
listeners here are permanent and *won't* be cleaned up when your script stops.

## Registering a Listener

Arcade provides a `register` extension function that allows you register
an event listener based on the type parameter provided. In the below example
we send a "Welcome!" message to the player whenever they join the server:

```kts
// This extension function is required
import net.casual.arcade.events.utils.register

suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    context.events.register<PlayerJoinEvent> { event ->
        event.player.sendSystemMessage(Component.literal("Welcome, ${event.player.username}!"))
    }
    awaitCancellation()
}
```

Remember to keep the script alive with `awaitCancellation()`; if `main` returns
the script stops and your listener goes with it.

To find a full list of listen-able events you can see all implementors of the
`ServerSideEvent` interface in IntelliJ.

For full details about how the event system works you can read
[arcade's documentation](https://arcade.casualchampionships.net/arcade-event-registry/listening.html).

## More Examples

Many events extend `CancellableEvent`, cancelling them stops the thing from
happening at all. Here we stop players from mining spawners:
```kts
context.events.register<PlayerBlockMinedEvent> { event ->
    if (event.state.isOf(Blocks.SPAWNER)) {
        event.cancel()
    }
}
```

Some cancellable events carry a result. For example, `PlayerJoinEvent`, cancelling it 
with a component kicks the player with that message:
```kts
context.events.register<PlayerJoinEvent> { event ->
    if (event.player.username == "Herobrine") {
        event.cancel(Component.literal("You are not welcome here"))
    }
}
```
