<br/>
<div align="center">
<h3 align="center">Kursive</h3>
<p align="center">
A Minecraft mod that allows you to run kotlin scripts
on the client and/or the server!
</div>

## About The Project

The idea of allowing scripts to be run on the Minecraft client originated when
[EssentialClient](https://github.com/senseiwells/EssentialClient) was first being
implemented. My scripting implementation achieved my goal, but it was poorly designed
and due to running on my own interpreted language had severe performance limitations.

After discovering kotlin scripting I decided that it would be an ideal language to
use for a scripting mod. It can compile directly into bytecode to run on the JVM
as well as not requiring wrapper APIs allowing scripts to interface directly with
Minecraft's code.

## Installing the Mod

You can build the mod from source, you will need Java 25 for this:
1. Clone the repo
   ```sh
   git clone https://github.com/senseiwells/Kursive.git
   ```
2. Compile the mod
   ```sh
   gradlew build
   ```
3. The build mod jar will be in `build/libs`, drag this into your mod folder.

## Getting Started with Scripting

This section will cover how to set up the environment to start
developing scripts locally.

### Installation

To get the project set up, clone to repo, and then we need to generate the
dependency jar which can then be used in IntelliJ to provide Intellisense.

You will need Java 25 and IntelliJ installed on your machine to get started.

1. Clone the repo
   ```sh
   git clone https://github.com/senseiwells/Kursive.git
   ```
2. Generate the dependency jar, this will publish to maven local
   ```sh
   gradlew publishKmc
   ```
   
### Creating a Script

To create a script you need to locate the `kursive/scripts` directory.
To create a server-side script, locate the `kursive/scripts` directory
in your world folder, and to create a client-side script it will be in your
`.minecraft` directory. Create a new file in the scripts directory named 
`<name>.main.kts`, for example: `test.main.kts`. You can then open this
file in IntelliJ.

A basic entrypoint script for a server-side script is as follows:
```kts
@file:DependsOn("me.senseiwells:kmc:0.2.0-alpha.8+26.2")

import kotlinx.coroutines.awaitCancellation
import me.senseiwells.kursive.api.ServerScriptContext
import net.minecraft.server.MinecraftServer

suspend fun main(server: MinecraftServer, context: ServerScriptContext) {
    awaitCancellation()
}
```

And for the client-side:
```kts
@file:DependsOn("me.senseiwells:kmc:0.2.0-alpha.8+26.2")

import kotlinx.coroutines.awaitCancellation
import me.senseiwells.kursive.api.ClientScriptContext
import net.minecraft.client.Minecraft

suspend fun main(minecraft: Minecraft, context: ClientScriptContext) {
    awaitCancellation()
}
```

From this entrypoint you can essentially write code as you would for a regular mod,
with some exceptions, registering global state, for example commands, or event listeners
should be done through the provided script context. This ensures that after your script
has finished running that everything will be appropriately cleaned up.

More details about this will be documented later...

### Running the Script

Once in game, you have access to the `kursive-client` and/or `kursive-server`
commands which will allow you to start and stop your scripts.

## Roadmap

- [ ] Implement script data storing api
- [ ] Implement script gui for client-sided scripts

## License

Distributed under the MIT License. See [MIT License](https://opensource.org/licenses/MIT) for more information.
