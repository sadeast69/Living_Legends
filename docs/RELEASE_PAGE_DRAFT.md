# Release Page Draft

## Short Description

World Remembers turns important moments in your Minecraft world into named
places.

Version `1.0.0` supports Minecraft `1.21.1` on Fabric `1.21.1` and NeoForge
`1.21.1`, with Java 21.

## What It Does

The mod watches for things that matter: deaths, fights, discoveries, portals,
mining, pet deaths, named mob deaths, settlements, and repeated visits. When
enough local history builds up, the world can save that location as a named
place.

Players can browse places in the World Journal and see a clean title overlay
when entering a saved place.

## Features

- Automatic named places from world events.
- Death sites, battlefields, landmarks, mining sites, settlements, memorials,
  boss sites, raid sites, and first discoveries.
- Six generated-name styles.
- Craftable World Journal.
- Place title overlay.
- Admin commands for create, rename, delete, import, export, and validation.
- Config options for balance and title behavior.
- Candidate decay so old unpromoted activity fades away.
- Storage, events, networking, title overlay, World Journal, commands, and
  data-driven compat registry support on both supported loaders.
- Data-driven compat registry foundation for datapacks and future addons.
- Fabric and NeoForge `1.21.1` loader builds.

## World Journal

Craft it with:

- book
- compass
- feather

Or test with:

```mcfunction
/give @s living_legends:world_journal
```

## Basic Commands

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

## Config

Config files are created in:

```text
config/world_remembers/living_legends/
```

Server owners can tune how often places appear, how title overlays behave, and
which place types are enabled.

## Compatibility

This release includes data-driven compat registries for bosses, structures,
biomes, mobs, blocks, and dimensions on both Fabric and NeoForge. Simple compat
packs can be datapacks and do not need a direct mod dependency.

## Known Limitations

- Fabric and NeoForge are both targeted at Minecraft `1.21.1`.
- No map or waypoint integration yet.
- Large third-party compat packs are not bundled yet.
- Balance may change in future updates.

## Backup Warning

Back up important worlds before installing or updating the mod on a live server.
