# Release Page Draft

## Short Description

World Remembers turns important moments in your Minecraft world into named
places.

Version `1.1.0` supports Minecraft `1.21.1` on Fabric `1.21.1` and NeoForge
`1.21.1`, with Java 21.

## Summary

World Remembers watches for events that matter: deaths, fights, discoveries,
portals, mining, pet deaths, named mob deaths, settlements, and repeated visits.
When enough local history builds up, the world can save that location as a named
place.

Players can browse remembered places in the World Journal and see a clean title
overlay when entering a saved place.

## Highlights In 1.1.0

- Built-in optional content compat packs for selected popular mods and
  datapacks.
- Better generated names in English and Russian for vanilla and compat places.
- Fall-death names now have deterministic variety instead of repeating one
  visible name.
- Naming audit tools better report fixed-pattern risks.
- JourneyMap, Xaero's Minimap, and FTB Chunks behavior remains unchanged.

## Built-in Compatibility

Map integrations:

- JourneyMap: discovered themed place labels, World Journal destinations, and
  Place Visual Themes.
- Xaero's Minimap: manual World Journal destination waypoints only.
- FTB Chunks: manual World Journal destination waypoints only.

Cross-loader content compat:

- The Aether
- Deeper and Darker
- Bosses of Mass Destruction
- Terralith
- Incendium
- Dungeons and Taverns
- When Dungeons Arise
- Towns and Towers
- Regions Unexplored
- Explorify
- Illager Invasion

Loader-specific content compat:

- NeoForge: L_Ender's Cataclysm, The Twilight Forest
- Fabric: BetterEnd, BetterNether, AdventureZ, VoidZ, Mythic Metals

Some compat entries are intentionally loader-specific because their target mods
are loader-specific in this release.

## Installation

Choose the jar that matches your loader:

- Fabric: `living_legends-fabric-1.1.0.jar`
- NeoForge: `living_legends-neoforge-1.1.0.jar`

Fabric builds require Fabric API. ModMenu is optional on Fabric.

Back up important worlds before installing or updating the mod on a live server.

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
/places debug compat summary
```

## Config

Config files are created in:

```text
config/world_remembers/living_legends/
```

Server owners can tune how often places appear, how title overlays behave, and
which place types are enabled.

## Known Limitations

- Fabric and NeoForge are both targeted at Minecraft `1.21.1`.
- Map integrations are optional and require the matching client map mod.
- Xaero and FTB Chunks compatibility is destination-only.
- Some content compat is loader-specific.
- Existing named places do not automatically rename themselves after a naming
  update.
- Balance may change in future updates.

