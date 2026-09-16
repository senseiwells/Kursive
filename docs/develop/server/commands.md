# Server Commands

Commands are one of the main ways users can directly interact with your scripts.
Kursive uses arcade's command system via `context.commands` to dynamically
register commands. Like everything else registered via the context commands
only exist for the lifetime of the script and are automatically removed when stopped.

Commands are built with [Brigadier](https://github.com/Mojang/brigadier), the
same library Minecraft uses for its own commands. Arcade adds a small Kotlin DSL
on top which makes building command trees much less verbose. For a full overview
of Minecraft's commands see the [fabric documentation](https://docs.fabricmc.net/develop/commands/basics)

## Registering a Command

```kts
suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    context.commands.register(CommandTree.buildLiteral("hello") {
        executes { ctx -> ctx.source.success("Hello!") }
    })
    awaitCancellation()
}
```

Once the script is running, any player can run `/hello`. Like events, the
command lives only as long as `main` is executing.

## Arguments and Subcommands

You can build up your command tree using `literal` and  `argument` nodes.
With arcade's command api it is pretty straight forward to see how they build
the command tree. With more large and complex commands you'll typically want
to split the command into its own `CommandTree` object, which can also be seen below:

```kts
val HealCommand = object: CommandTree<CommandSourceStack> {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral<CommandSourceStack>("heal") {
            argument("player", EntityArgument.player()) {
                // /heal <player>
                executes(::healPlayer)
                argument("amount", IntegerArgumentType.integer(1)) {
                    // /heal <player> <amount>
                    executes(::healPlayerWithAmount)
                }
            }
            literal("all") {
                // /heal all
                executes(::healAll)
            }
        }
    }

    private fun healPlayer(context: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(context, "player")
        player.heal(player.maxHealth)
        return context.source.success("Healed ${player.username}")
    }

    private fun healPlayerWithAmount(context: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(context, "player")
        val amount = IntegerArgumentType.getInteger(context, "amount").toFloat()
        player.heal(amount)
        return context.source.success("Healed ${player.username}")
    }

    private fun healAll(context: CommandContext<CommandSourceStack>): Int {
        var healed = 0
        for (player in ctx.source.server.players) {
            player.heal(player.maxHealth)
            healed++
        }
        context.source.success("Healed everyone")
        return healed
    }
}

suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    context.commands.register(HealCommand)
    awaitCancellation()
}
```

## Permissions

Restrict who can run a command with `requiresPermission`:

```kts
context.commands.register(CommandTree.buildLiteral<CommandSourceStack>("nuke") {
    // Permission.GAMEMASTERS -> op level 2
    requiresPermission(PermissionLevel.GAMEMASTERS)
    executes { ctx -> /* ... */ }
})
```

This accepts `PermissionLevel`, but also `Identifier`s which can be used if the
server has a permissions mod installed.
