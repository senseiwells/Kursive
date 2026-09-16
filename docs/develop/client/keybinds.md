# Client Keybinds

Keybinds are a great way for client-sided scripts to take input from a player.
Kursive provides a simple api to easily register your own keybinds through 
`context.keybinds`, which will automatically get removed when the script stops.

## Registering a Keybind

Give the keybind an identifier, a display name, and optional default keys:

```kts
suspend fun main(minecraft: Minecraft, context: ClientScriptContext) {
    val keybind = context.keybinds.register(
        Identifier("myscript", "toggle"),
        Component.literal("Toggle Thing"),
        InputConstants.KEY_G
    )
    keybind.addListener(KeybindListener.onPress {
        minecraft.player?.sendSystemMessage(Component.literal("Pressed!"))
    })
    awaitCancellation()
}
```

Key constants come from `InputConstants`. Passing more than one key makes
it a multi-keybind, and the keybind will only trigger when *all* keys are pressed.

```kts
context.keybinds.register(id, name, InputConstants.KEY_LCONTROL, InputConstants.KEY_G)
```

## Listening

`KeybindListener` has three hooks, and there's a static helper for each if you
only care about one:

```kts
keybind.addListener(KeybindListener.onPress { /* keys went down */ })
keybind.addListener(KeybindListener.onRelease { /* keys came up */ })
keybind.addListener(KeybindListener.onSetKeys { keys -> /* rebound in controls screen */ })
```

Or implement the interface to handle several at once:

```kts
keybind.addListener(object: KeybindListener {
    override fun onPress() { /* ... */ }
    override fun onRelease() { /* ... */ }
})
```

## Polling

Instead of listening, you can query the keybind's state from a tick event:

```kts
context.events.register<ClientTickEvent> {
    if (keybind.isHeld) {
        // Do something while held
    }
    repeat(keybind.consumeClicks()) {
        // Do something once per press since last tick
    }
}
```

## Controls Screen

By default, a script's keybind isn't rebindable from Minecraft's controls screen.
To list it there, pick a category and add it:

```kts
context.keybinds.listInControlsScreen(KeyMapping.Category.MISC, keybind)
```

## Serializing

You can use Kursive's [persistent data storage](../common/persistent-data.md) to
remember what keys a user set for a specific keybind:

```kts
val keybindsStore = context.stores.private("keybinds")
val testKeys = keybindsStore.property("test_keys", InputKeys.CODEC, InputKeys.EMPTY) // Empty default key

val testKeybind = context.keybinds.register(Identifier("myscript", "test"), Component.literal("Test"), testKeys.get())
testKeybind.addListener(KeybindListener.onSetKeys(testKeys::set))

context.keybinds.listInControlsScren(KeyMapping.Category.MISC, testKeybind)
```