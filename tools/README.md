# Test tools

`regress.py` places every trap on a dev server, sets each one off with mobs and
checks the result (16 checks).

1. Copy `server.properties` into `run/` (it makes a tall superflat world with the
   grass at y=40, turns on RCON with password `test`, and stops the server from
   pausing when nobody is online) and accept the EULA: `echo eula=true > run/eula.txt`.
2. Start the server: `./gradlew runServer`
3. Once it says `RCON running`, set the world up and run the checks:

```
python3 tools/rcon.py "forceload add -112 -112 112 112" "time set midnight" "gamerule advance_time false" "gamerule spawn_mobs false"
python3 tools/regress.py
```

To watch the railgun's arrow tick by tick, start the server with
`-Dtrapplacer.debugArrows=true` in `JAVA_TOOL_OPTIONS`; arrows and about-to-explode
TNT are written to the server log.
