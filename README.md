# World Remembers: Living Legends

World Remembers turns important moments in your world into named places.

If players die in the same valley, the world may remember it as a death site.
If a raid or long fight happens near a village, it can become a battlefield.
Discoveries, portals, mines, pets, named mobs, settlements, and often-visited
spots can also become places with names, journal entries, and title overlays.

This is the `1.0.0` release for Minecraft `1.21.1`. It supports Fabric
`1.21.1`, NeoForge `1.21.1`, and Java 21. Back up important worlds before
installing or updating the mod.

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
- World Journal item for browsing remembered places.
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
- Optional ModMenu support for the Fabric client title overlay settings screen.

## Current Support

- Minecraft `1.21.1`
- Fabric `1.21.1` with Fabric Loader `0.16.14+` and Fabric API
  `0.116.11+1.21.1`
- NeoForge `1.21.1` with NeoForge `21.1.x`
- Java 21
- Mod id: `living_legends`

World Journal, title overlay, commands, storage, events, networking, and
data-driven compat registries are available on both supported loaders.

## Installation

Choose the jar that matches your loader:

- Fabric users install `living_legends-fabric-1.0.0.jar`.
- NeoForge users install `living_legends-neoforge-1.0.0.jar`.

Back up important worlds before installing or updating.

## Quick Start

Install the jar for your loader, start a world or server, and let things happen.
Explore, fight, die, use portals, mine valuables, discover structures, and
revisit places.

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

Back up worlds before installing, updating, or changing important config.

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

- Fabric and NeoForge are both targeted at Minecraft `1.21.1`.
- No map or waypoint integration yet.
- Large third-party compat packs are not bundled yet.
- Balance may change in future updates.

## Documentation

- [Commands](docs/COMMANDS.md)
- [Config](docs/CONFIG.md)
- [Compat API](docs/COMPAT_API.md)
- [Porting notes](docs/PORTING_NOTES.md)
- [Release notes](docs/RELEASE_NOTES.md)

## Building

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The release jars are produced by the loader modules:

- `fabric/build/libs/living_legends-fabric-1.0.0.jar`
- `neoforge/build/libs/living_legends-neoforge-1.0.0.jar`

## License

World Remembers: Living Legends is released under the MIT License. That means
other mod authors may make ports, compatibility addons, datapacks, forks, and
pull requests, as long as the license notice is kept.
