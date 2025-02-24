# EssentialScripting

## Todo

- Fix weird remapping issue with MinecraftServer#playerList??

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

The janky way I get sources to work:

- Compile fat jar
- Create new intellij project in the scripts directory
- Set the source dir to be the root dir
- Add jar dependency - link the fat jar