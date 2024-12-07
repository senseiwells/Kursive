# EssentialScripting

## Todo

- Don't recompile scripts unless they're modified

```kotlin
fun main() {
    
}

fun main(args: Array<String>) {
    
}

fun main(client: Minecraft) {
    
}

fun main(client: Minecraft, args: Array<String>) {
    
}

fun main(server: MinecraftServer) {
    
}

fun main(server: MinecraftServer, args: Array<String>) {
    
}

fun main(client: Minecraft) = launch(client) {
    
}
```