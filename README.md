# Trap Placer

A Fabric mod for **Minecraft 1.21.11** that builds complete, working traps in about two seconds.
Aim the Trap Placer at the ground, right-click, and the trap rises out of the ground already wired
up: hidden pistons, observers, buried pits and loaded dispensers.

The traps are in the style of **JudeLow**'s SMP traps (innocent-looking bait with hidden
redstone underneath), plus a **one-shot arrow railgun** based on **ProfessorTNT**'s
[How to build ONE SHOT ARROW RAILGUNS in Minecraft](https://www.youtube.com/watch?v=4sTQgv2uCBo).

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5+ for Minecraft 1.21.11.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and `trapplacer-1.0.0.jar` in your `mods` folder.

On a server, both the server and every player need the mod.

## Using the Trap Placer

* **Get one:** it's in the *Tools & Utilities* creative tab, `/trap give`, or crafted:

  ```
   T      T = TNT
  PRP     P = Piston   R = Block of Redstone
   S      S = Stick
  ```
* **Right-click the ground** to build the selected trap. It's built on the block you're
  looking at (up to 64 blocks away) and extends away from you, in the direction you face.
* **Sneak + right-click** to switch to the next trap.
* While you hold it, a **particle outline** shows where the trap will go. The yellow edge is
  the far side.
* `/trap undo` takes your last trap down again and puts the ground back.

Each trap takes about 2 seconds to build: it rises layer by layer, and the redstone switches
on at the end. Visible ground blocks match the terrain you build on (grass, sand, stone…), so
the traps blend in.

## The traps

| Trap | Looks like | Set off by | What happens |
|---|---|---|---|
| **Front Door Drop** | A little hut with an iron double door and pressure plates | Stepping on a plate and walking in | The doors open as normal, then the carpet just inside drops away: a 15-block fall onto stalagmites. The floor closes again behind them. |
| **Loot Chest Trap** | A double chest on a mossy patch | Opening the chest | The ground in front of the chest disappears into a stalagmite pit, then closes over it. |
| **Crafting Table Trap** | A little camp: crafting table, furnace, barrel | Breaking (stealing) the crafting table, or smelting in the furnace | Same stalagmite pit. It stays open until a crafting table is put back, which closes it. |
| **Sweet Dreams Bed Trap** | A cozy cabin with a bed | Lying down in the bed | 12 TNT under the floor go off while they sleep. |
| **Fake Diamond Mine** | A rocky outcrop with a mine tunnel and a diamond at the end | Mining the diamond ore | A piston and slime block slam the tunnel shut behind the miner, and the ceiling pours lava. |
| **Powder Snow Pit** | A patch of snow | Walking on it | It's powder snow over a 13-block drop onto stalagmites. Leather boots let you walk across. |
| **One-Shot Arrow Railgun** | A glass tower with a barrel | Pressing the button on the post at the back | About 4 seconds later, 27 TNT launch one arrow at ~20 blocks per tick. That's about 40 damage, enough to one-shot a player in full diamond. It comes loaded with 4 shots; refill the dispensers to reload. |

The pit traps re-arm themselves, but the carpet that fell in is gone. Put new carpet back on
the piston heads to hide them again.

### How the railgun works

An arrow's damage scales with its speed, and TNT explosions push arrows. The railgun:

1. Dispensers around a 1×1 water shaft all drop TNT at once. Every TNT sinks to the same spot
   at the bottom, so they explode at a single point instead of knocking each other around.
   Because they explode underwater, no blocks are destroyed.
2. A dispenser shoots an arrow down onto an iron trapdoor in front of the shaft, where it sticks.
3. 82 ticks after the button, a long repeater line opens that trapdoor. The arrow comes loose
   and hangs in the air for a moment, and the TNT goes off exactly then, firing it through
   the barrel.

It shoots flat. Expect up to about 1.5° of random sideways spread per shot, so aim it
at something within about 25 blocks (a doorway or a path).

## Commands

| Command | Who | |
|---|---|---|
| `/trap list` | everyone | All traps with a short description |
| `/trap info <trap>` | everyone | Description, trigger and survival cost |
| `/trap select <trap>` | everyone | Pick the trap on the Trap Placer you're holding |
| `/trap undo` | everyone | Remove your last trap (last 10 are remembered until the server restarts) |
| `/trap give [players]` | ops | Give a Trap Placer |
| `/trap place <trap> <x y z> [north/east/south/west]` | ops | Build a trap without a wand or materials (also works from the server console) |

Trap ids: `front_door`, `loot_chest`, `crafting_table`, `bed`, `diamond_mine`, `powder_snow`, `arrow_railgun`.

## Survival

In creative, traps are free. In survival you pay for the blocks and the dispenser contents.
The trap is only built if you have everything, and `/trap info <trap>` lists the cost.
Blocks that copy the surrounding ground (the hidden fill and pit walls) are free.

| Trap | Main costs |
|---|---|
| Front Door Drop | 72 dark oak slabs, 58 spruce planks, 18 red carpet, 14 redstone, 6 repeaters, 4 pistons, 2 iron doors… |
| Loot Chest Trap | 26 moss carpet, 22 moss blocks, 20 redstone, 6 repeaters, 4 pistons, 4 pointed dripstone, 2 trapped chests… |
| Crafting Table Trap | 25 moss carpet, 20 moss blocks, 18 redstone, 4 pistons, 2 observers, 1 waxed copper bulb, 1 comparator… |
| Sweet Dreams Bed Trap | 56 oak slabs, 49 oak planks, 12 TNT, 12 redstone, 1 observer, 1 bed… |
| Fake Diamond Mine | ~520 stone-type blocks, 3 lava buckets, 3 dispensers, 1 diamond, 1 piston, 1 slime block… |
| Powder Snow Pit | 9 powder snow buckets, 9 pointed dripstone, 9 dripstone blocks |
| One-Shot Arrow Railgun | 251 glass, 108 TNT (4 shots), 88 redstone, 28 dispensers, 16 iron blocks, 15 repeaters, 16 arrows… |

## Config

`config/trapplacer.properties` is created the first time the game or server starts:

| Setting | Default | |
|---|---|---|
| `survivalRequiresMaterials` | `true` | Set to `false` to make traps free in survival too |
| `buildTicks` | `40` | How long building takes (20 ticks = 1 s, `0` = instant) |
| `maxReach` | `64` | How far away the wand can place a trap |

## Building from source

Needs Java 21.

```
./gradlew build
```

The mod jar ends up in `build/libs/`. `tools/` has a script that places every trap on a dev
server, sets each one off and checks that it worked (see `tools/README.md`).

## Credits

Trap ideas inspired by JudeLow's trap videos. The railgun concept comes from ProfessorTNT's
arrow railgun video. The builds in this mod are original designs, and this project isn't
affiliated with either creator.
