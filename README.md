<br/>
<div align="center">
<a href="https://github.com/senseiwells/Kursive">
<img src="./src/main/resources/assets/kursive/icon.png" alt="Logo" width="80" height="80">
</a>
<h3 align="center">Kursive</h3>
<p align="center">
A Minecraft mod that allows you to run kotlin scripts on the client and the server on the fly!
</div>

## About the Project

The idea of allowing scripts to be run on the Minecraft client originated when
[EssentialClient](https://github.com/senseiwells/EssentialClient) was first being
implemented. My scripting implementation achieved my goal, but it was poorly designed
and due to running on my own interpreted language had severe performance limitations.

After discovering kotlin scripting I decided that it would be an ideal language to
use for a scripting mod. It can compile directly into bytecode to run on the JVM
as well as not requiring wrapper APIs allowing scripts to interface directly with
Minecraft's code.

## Installing the Mod

You can install the mod from [modrinth](https://modrinth.com/mod/kursive), alternatively
you can build the mod from source.

### Building from Source

You can build the mod from source, you will need Java 25 for this:
1. Clone the repo
   ```sh
   git clone https://github.com/senseiwells/Kursive.git
   ```
2. Compile the mod
   ```sh
   gradlew build
   ```
3. The build mod jar will be in `build/libs`

Once you have the mod jar drag it into your mod folder. 
You will also need to install all of Kursive's dependencies:
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
- [YACL](https://modrinth.com/mod/yacl) (client-side only)

## Documentation

The mod's documentation can be found [here](https://kursive.senseiwells.me).

The documentation details how to start developing your first scripts as well as
detailing how to run scripts on the client and server.

## License

Distributed under the MIT License. See [MIT License](https://opensource.org/licenses/MIT) for more information.
