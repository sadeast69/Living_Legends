# Release Notes

## 1.1.0

World Remembers: Living Legends `1.1.0` is a content compatibility and naming
polish release for Minecraft `1.21.1` on Fabric and NeoForge.

Back up important worlds before updating. Saved data is versioned and migrated
on load, but backups are still the safest way to protect long-running worlds.

## Highlights

- Built-in optional content compat packs for selected boss, biome, structure,
  mob, block, and dimension ids.
- Cross-loader compat for The Aether, Deeper and Darker, Bosses of Mass
  Destruction, Terralith, Incendium, Dungeons and Taverns, When Dungeons Arise,
  Towns and Towers, Regions Unexplored, Explorify, and Illager Invasion.
- NeoForge compat for L_Ender's Cataclysm and The Twilight Forest.
- Fabric compat for BetterEnd, BetterNether, AdventureZ, VoidZ, and Mythic
  Metals.
- Improved English and Russian generated-name text for vanilla and compat
  names.
- Fixed fall-death naming variety so repeated fall-death places are not all
  assigned the same visible name.
- Improved naming audits for fixed-pattern pools.
- JourneyMap, Xaero's Minimap, and FTB Chunks behavior remains unchanged from
  the accepted map compatibility release.

## Current Support

- Minecraft `1.21.1`
- Fabric `1.21.1`
- NeoForge `1.21.1`
- Java 21
- Fabric API required for Fabric builds
- ModMenu optional on Fabric

## Notes For Existing Worlds

Existing place records remain stable. Named places do not automatically rename
themselves after a naming update. To see new generated names, create new places
or use the regenerate command where appropriate.

Recommended checks after updating:

```mcfunction
/places debug selftest
/places debug validate
/places debug compat summary
/places export
```

## Known Limitations

- Map integrations are optional and require matching client map mods.
- JourneyMap receives discovered labels and destinations; Xaero and FTB Chunks
  are destination-only.
- Some content compat is loader-specific. Fabric includes Fabric-only and
  cross-loader compat; NeoForge includes NeoForge-specific and cross-loader
  compat.
- Balance may change in future updates.
- Journal art and small UI details may still be refined later.

## Previous Release

`1.0.0` was the first public release for Fabric and NeoForge `1.21.1`, including
the World Journal, place title overlay, `/places` commands, storage, events,
networking, diagnostics, JourneyMap labels/destinations, Xaero destination
support, and FTB Chunks destination support.

