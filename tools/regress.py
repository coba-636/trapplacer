"""Places every trap, sets it off and checks the result. Needs the dev server running."""
import os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from rcon import Rcon
r = Rcon()
GROUND = 40
results = []

def rot(facing, x, y, z):
    """Local (x right, y up, z forward) -> world offset, matching Placement."""
    cx, cy, cz = x, y, -z
    if facing == 'north': return (cx, cy, cz)
    if facing == 'east': return (-cz, cy, cx)
    if facing == 'south': return (-cx, cy, -cz)
    if facing == 'west': return (cz, cy, -cx)

class Site:
    def __init__(self, trap, ox, oz, facing):
        self.trap, self.ox, self.oz, self.facing = trap, ox, oz, facing
    def w(self, x, y, z):
        dx, dy, dz = rot(self.facing, x, y, z)
        return (self.ox + dx, GROUND + dy, self.oz + dz)
    def at(self, x, y, z):
        a, b, c = self.w(x, y, z)
        return f"{a} {b} {c}"
    def center(self, x, y, z, dy=0.0):
        a, b, c = self.w(x, y, z)
        return f"{a + 0.5} {b + dy} {c + 0.5}"

def check(name, cond):
    results.append((name, cond))
    print(('PASS ' if cond else 'FAIL ') + name)

def ok(cmd):
    return 'passed' in r.cmd(cmd)

def victim(tag, pos):
    r.cmd(f"summon zombie {pos} {{PersistenceRequired:1b,Tags:[\"{tag}\"],attributes:[{{id:\"minecraft:movement_speed\",base:0.0}}]}}")

def alive(tag):
    return ok(f"execute if entity @e[tag={tag}]")

r.cmd("kill @e[type=!player]")
sites = {
    'front_door': Site('front_door', -60, -60, 'east'),
    'loot_chest': Site('loot_chest', -20, -60, 'south'),
    'crafting_table': Site('crafting_table', 20, -60, 'west'),
    'bed': Site('bed', 60, -60, 'north'),
    'diamond_mine': Site('diamond_mine', -60, 40, 'south'),
    'powder_snow': Site('powder_snow', -20, 40, 'east'),
    'arrow_railgun': Site('arrow_railgun', 40, 60, 'west'),
}
for s in sites.values():
    print(r.cmd(f"trap place {s.trap} {s.ox} {GROUND} {s.oz} {s.facing}"))
    time.sleep(2.5)
time.sleep(1)

# Front door: zombie on the drop floor, NoAI zombie on a plate.
s = sites['front_door']
victim('fd', s.center(0, 1, 2, 0.1))
time.sleep(0.5)
check('front_door victim standing', alive('fd'))
r.cmd(f"summon zombie {s.center(1, 1, 0)} {{NoAI:1b,PersistenceRequired:1b,Tags:[\"fdp\"]}}")
time.sleep(0.6)
check('front_door door opens', ok(f"execute if block {s.at(1, 1, 1)} iron_door[open=true]"))
time.sleep(3)
check('front_door victim dead', not alive('fd'))
r.cmd("kill @e[tag=fdp]")

# Loot chest: power under the chest like an opened trapped chest does.
s = sites['loot_chest']
victim('lc', s.center(1, 1, 2, 0.1))
time.sleep(0.5)
r.cmd(f"setblock {s.at(0, 0, 3)} redstone_block")
time.sleep(3)
check('loot_chest victim dead', not alive('lc'))
r.cmd(f"setblock {s.at(0, 0, 3)} moss_block")
time.sleep(2)
check('loot_chest floor resets', ok(f"execute if block {s.at(0, 0, 2)} piston_head"))

# Crafting table: steal the table.
s = sites['crafting_table']
victim('ct', s.center(0, 1, 1, 0.1))
time.sleep(0.5)
r.cmd(f"setblock {s.at(0, 1, 3)} air")
time.sleep(3)
check('crafting_table victim dead', not alive('ct'))
check('crafting_table stays open', not ok(f"execute if block {s.at(0, 0, 1)} piston_head"))
r.cmd(f"setblock {s.at(0, 1, 3)} crafting_table")
time.sleep(2)
check('crafting_table resets when table is replaced', ok(f"execute if block {s.at(0, 0, 1)} piston_head"))

# Bed: occupy the bed.
s = sites['bed']
victim('bd', s.center(1, 1, 3))
time.sleep(0.5)
r.cmd(f"setblock {s.at(0, 1, 3)} red_bed[part=head,facing={s.facing},occupied=true]")
time.sleep(0.5)
out = r.cmd("execute if entity @e[type=tnt]")
check('bed lights all 12 TNT (' + out.strip() + ')', 'Count: 12' in out)
time.sleep(5)
check('bed victim dead', not alive('bd'))

# Diamond mine: mine the ore.
s = sites['diamond_mine']
victim('dm', s.center(0, 1, 4))
time.sleep(0.5)
r.cmd(f"setblock {s.at(0, 2, 6)} air")
time.sleep(1.5)
check('diamond_mine tunnel sealed', not ok(f"execute if block {s.at(0, 1, 2)} air") and not ok(f"execute if block {s.at(0, 2, 2)} air"))
check('diamond_mine lava poured', ok(f"execute if block {s.at(0, 2, 4)} lava"))
time.sleep(6)
check('diamond_mine victim dead', not alive('dm'))

# Powder snow: just stand on it.
s = sites['powder_snow']
victim('ps', s.center(0, 1, 1, 0.2))
time.sleep(6)
check('powder_snow victim dead', not alive('ps'))

# Railgun: target 20 blocks down range, press the button.
s = sites['arrow_railgun']
r.cmd(f"summon zombie {s.center(0, 1, 20)} {{NoAI:1b,PersistenceRequired:1b,Tags:[\"rg\"]}}")
r.cmd(f"setblock {s.at(0, 2, -7)} redstone_block")
time.sleep(1)
r.cmd(f"setblock {s.at(0, 2, -7)} polished_andesite")
time.sleep(5.5)
check('arrow_railgun one-shots target at 20 blocks', not alive('rg'))
check('arrow_railgun destroyed no blocks', ok(f"execute if block {s.at(0, 15, 0)} iron_block") and ok(f"execute if block {s.at(0, 2, 1)} iron_trapdoor"))

# Imported schematics: pasted exactly, with their saved contents and entities.
print(r.cmd(f"trap place duck_railgun -60 {GROUND} -20 north"))
time.sleep(3)
print(r.cmd(f"trap place orbital_arrow_cannon 60 {GROUND} -20 north"))
time.sleep(4)
check('duck_railgun wind charges loaded', 'count: 60' in r.cmd("data get block -68 44 -22 Items"))
check('duck_railgun fire button in place', ok("execute if block -65 47 -23 stone_button[face=floor]"))
out = r.cmd("execute if entity @e[type=arrow,x=58.5,y=85.5,z=-32.5,distance=..1.5]")
check('orbital_arrow_cannon has its 194 arrows (' + out.strip() + ')', 'Count: 194' in out)
time.sleep(5)
out = r.cmd("execute if entity @e[type=arrow,x=58.5,y=85.5,z=-32.5,distance=..1.5]")
check('orbital_arrow_cannon arrows stay in their stasis', 'Count: 194' in out)

print(r.cmd(f"trap place popup_arrow_cannon 0 {GROUND} 20 north"))
time.sleep(3)
out = r.cmd("data get block 1 46 19 Items")
check('popup_arrow_cannon wind-charged potions loaded', out.count('minecraft:wind_charged') == 7)
check('popup_arrow_cannon button in place', ok("execute if block 2 46 18 oak_button[face=wall,facing=east]"))
# It was saved with its observer clock running, so the piston it drives keeps moving.
states = set()
for _ in range(12):
    states.add(ok("execute if block 0 44 17 sticky_piston[extended=false]"))
    time.sleep(0.15)
check('popup_arrow_cannon clock is running', states == {True, False})

# Dug into the ground: its grass layer replaces the clicked block, the rest is buried.
print(r.cmd(f"trap place flat_ground -80 {GROUND} 80 north"))
time.sleep(4)
check('flat_ground is flush with the ground', ok(f"execute if block -80 {GROUND} 80 grass_block") and ok(f"execute if block -80 {GROUND + 1} 77 oak_pressure_plate"))
check('flat_ground shaft is buried under the strip', ok(f"execute if block -80 {GROUND - 4} 73 oak_sign"))
victim('fg', f"-79.5 {GROUND + 1} 77.5")
time.sleep(1.5)
check('flat_ground plate sinks the strip', ok(f"execute if block -79 {GROUND} 73 air"))
# Step off rather than die: a dropped item would hold the wooden plate down.
r.cmd(f"tp @e[tag=fg] -70.5 {GROUND + 1} 85.5")
time.sleep(2.5)
check('flat_ground resets when the plate is released', ok(f"execute if block -79 {GROUND} 73 dirt"))

print()
print(f"{sum(1 for _, c in results if c)}/{len(results)} checks passed")
