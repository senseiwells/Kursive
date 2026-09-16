# Client-side vs Server-side Scripting

Kursive lets you write scripts for both the client and the server. But before you
start writing a script it's important to understand the difference between the
two, as it decides where your script will run and what it will be able to do.

Minecraft is split into two halves, a server-side and a client-side. Each half
has a different job, and a script written for one won't work on the other.

## What is "server-side"?

Minecraft's server-side is what actually runs the physics and processes the world;
ticking the world, spawning/simulating mobs and updating blocks. The server doesn't
render anything, it's job is to take inputs from connected players, and tell
any connected clients what is happening in the world so they can render it.

## What is "client-side"?

Minecraft's client-side is what you directly interact with. You can think of it
as a middleman between you, the player, and the server. The client is responsible
for telling the server about your inputs (e.g. movement, attacking, interacting)
and for rendering the game to the screen.

The client only keeps a temporary copy of a slice of the actual Minecraft world
that the server has. Anything outside your render distance simply doesn't exist
on the client. The client only really has power over you, the player. Everything
else is dictated by the server.

## What about singleplayer?

Although singleplayer doesn't run a dedicated server, it still uses this exact
architecture internally. When you open a singleplayer world your game starts an
"internal" Minecraft server, and your client connects to it just as it would to
a remote one. This means that even in singleplayer the distinction still matters:
server-side scripts run on the internal server, and client-side scripts run on
your client.

## Which side should you script on?

Which side your script belongs on depends on what you are trying to do. Typically:

**Your script should be server-sided when:** You want to interact with the world. 
This includes placing/breaking blocks, spawning mobs, or modifying game logic.

**Your script should be client-sided when:** You want to change how the game looks, or
you want to automate things your player can do.

It may be the case that what you are trying to achieve may need both a client-side **and**
server-side script to work, for example pressing keybinds to teleport to specific waypoints
in the world. 

In the above example you would need a client-side script to detect keybinds
being pressed, then tell the server that you want to teleport somewhere. The server-script
would then act on this request and actually teleport the player. Technically this example
could be achieved by the client simulating the player running a `/tp` command, which 
effectively works as sending a request to the server, but this assumes the player has 
permission to run that command and won't work for features which don't have an existing 
command.

Once you've decided, head over to [Creating Scripts](./creating-scripts.md) to
see how to set up a script for each side.
