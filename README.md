# EssentialScripting

Getting started:
```
./gradlew publishMces
```

This will publish the scripting dependency to your maven local
which you can use for development.

```kt
@file:DependsOn("me.senseiwells:mces:0.2.0-alpha.1+26.1")

import kotlinx.coroutines.awaitCancellation
import me.senseiwells.scripting.api.ServerScriptContext
import net.minecraft.server.MinecraftServer

suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    awaitCancellation()
}
```