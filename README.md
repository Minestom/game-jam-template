# Game Jam Template

This is the template for the 2025 Minestom Game Jam!

It is **not** mandatory to use this template - it's just here to help teams get working on the actual game more quickly.

What **is** mandatory for your submission is:
- The submission must use Minestom
- You must submit a JAR that runs on its own (we recommend a fat/shadow jar to bundle dependencies) as well as a ZIP file containing relevant files (e.g. world files)

This template uses Minestom and includes the shadowJar plugin (run with `./gradlew shadowJar`) so these requirements are
already fulfilled.

The template currently implements:
- Anvil world loading for the lobby (in `./lobby/`) and for the game world (in `./game/`)
- Block handlers that send the correct NBT to the client
- A queue system
- A game loop

You can easily remove these if they're not desired; for example, to remove the queue, just delete Queue.java (and fix the few related compiler errors).
