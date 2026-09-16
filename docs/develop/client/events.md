# Client Events

Being able to listen to events is a fundamental feature. Kursive uses   
[Arcade](https://github.com/CasualChampionships/arcade)'s event system and is
provided through `context.events`. All client events live under 
`net.casual.arcade.events.client` and implement `ClientSideEvent`.

While typically fabric mods use events provided by fabric api for example:
`ClientTickEvents.START_CLIENT_TICK`, these should be avoided as registered
listeners here are permanent and *won't* be cleaned up when your script stops.

## Registering a Listener

Arcade provides a `register` extension function that allows you register
an event listener based on the type parameter provided. In the below example
we send a "Welcome back!" message to the player whenever the join a world/server:

```kts
// This extension function is required
import net.casual.arcade.events.utils.register

suspend fun main(minecraft: Minecraft, context: ClientScriptContext) {
    context.events.register<PlayerJoinEvent> { event ->
        event.player.sendSystemMessage(Component.literal("Welcome back!"))
    }
    awaitCancellation()
}
```

Remember to keep the script alive with `awaitCancellation()`; if `main` returns
the script stops and your listener goes with it.

To find a full list of listen-able events you can see all implementors of the
`ClientSideEvent` interface in IntelliJ.

For full details about how the event system works you can read 
[arcade's documentation](https://arcade.casualchampionships.net/arcade-event-registry/listening.html).

## More Examples

One of the most useful client events is `GuiRenderEvent` as that allows you to
hook into the gui rendering code, letting you render things to the HUD:
```kts
context.events.register<GuiRenderEvent> { event ->
    val player = minecraft.player ?: return@register
    val text = "Health: ${player.health.toInt()}"
    event.graphics.text(minecraft.font, text, 4, 4, 0xFFFFFFFF.toInt())
}
```

We can also intercept serverbound packets and prevent them from being sent by cancelling
the event or replacing the packet being sent entirely:
```kts
context.events.register<ServerboundPacketEvent> { event ->
    val packet = event.packet
    if (packet is ServerboundAttackPacket) {
        event.cancel()
    } else if (packet is ServerboundChatPacket) {
        event.packet = ServerboundChatPacket(
            "I replaced your message!", packet.timeStamp, packet.salt, null, packet.lastSeenMessages
        )
    }
}
```

## Missing Events

Arcade doesn't have a very long list of available `ClientSideEvent`s, if there's
an event that you'd like to be implemented either make an issue report on 
[the GitHub](https://github.com/senseiwells/Kursive) 
or send a message on [my discord](https://discord.gg/7R9SfktZxH).