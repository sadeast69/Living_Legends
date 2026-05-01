# World Remembers: Living Legends

World Remembers turns important moments in your world into named places.

If players die in the same valley, the world may remember it as a death site.
If a raid or long fight happens near a village, it can become a battlefield.
Discoveries, portals, mines, pets, named mobs, settlements, and often-visited
spots can also become places with names, journal entries, and title overlays.

This is a `1.0.0-rc.1` release candidate. Back up important worlds before using
it on a live server.

## Features

- Automatic named places from player actions and world events.
- Place types such as death sites, battlefields, mining sites, landmarks,
  settlements, boss sites, raid sites, pet memorials, named mob memorials, and
  first discoveries.
- Generated names with six built-in styles:
  - `vanilla_adventure`
  - `neutral_server`
  - `dark_fantasy`
  - `cozy_survival`
  - `epic_mythology`
  - `funny_community`
- World Journal / Журнал Мира item for browsing remembered places.
- Place title overlay when a player enters a saved place.
- `/places` commands for lookup, admin tools, import/export, and diagnostics.
- Candidate decay for stale unpromoted activity. Existing named places do not
  decay.
- General landmark spacing so the whole map does not turn into generic walking
  spots.
- Deleted place suppression, config validation, save-data migrations, and
  debug validation tools.
- Data-driven compat registries for bosses, structures, biomes, mobs, blocks,
  and dimensions.
- Optional ModMenu support for the client title overlay settings screen.

## Current Support

- Minecraft `1.21.1`
- Fabric Loader `0.16.14+`
- Fabric API `0.116.11+1.21.1`
- Java 21
- Mod id: `living_legends`

The NeoForge module is only a placeholder right now. Do not treat it as a
public NeoForge release.

## Quick Start

Install the Fabric jar, start a world or server, and let things happen. Explore,
fight, die, use portals, mine valuables, discover structures, and revisit places.

Craft the World Journal with:

- `minecraft:book`
- `minecraft:compass`
- `minecraft:feather`

For testing:

```mcfunction
/give @s living_legends:world_journal
```

Useful first commands:

```mcfunction
/places list
/places nearest
/places info nearest
```

If something looks wrong:

```mcfunction
/places debug selftest
/places debug validate
```

## For Server Owners

Back up worlds before RC testing.

Config files live in:

```text
config/world_remembers/living_legends/
```

Useful admin checks:

```mcfunction
/places debug selftest
/places debug validate
/places export
```

Use `/places export` before risky config changes or before testing migration
behavior on an important world.

See [docs/CONFIG.md](docs/CONFIG.md) for practical tuning notes.

## For Mod Authors

World Remembers is MIT licensed. Ports, fixes, pull requests, compatibility
datapacks, and small compat modules are welcome.

Simple compat does not need a direct mod dependency. Datapacks can add or
override boss, structure, biome, mob, block, and dimension themes.

See [docs/COMPAT_API.md](docs/COMPAT_API.md).

## Known Limitations

- Fabric `1.21.1` only for this release candidate.
- No supported NeoForge release yet.
- No map or waypoint integration yet.
- Large third-party compat packs are not bundled yet.
- Balance may change during RC testing.

## Documentation

- [Commands](docs/COMMANDS.md)
- [Config](docs/CONFIG.md)
- [Compat API](docs/COMPAT_API.md)
- [Porting notes](docs/PORTING_NOTES.md)
- [Release notes](docs/RELEASE_NOTES.md)
- [Pre-1.0 checklist](docs/PRE_1_0_CHECKLIST.md)

## Building

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The release jar is produced by the Fabric module.

## License

World Remembers: Living Legends is released under the MIT License. That means
other mod authors may make ports, compatibility addons, datapacks, forks, and
pull requests, as long as the license notice is kept.
