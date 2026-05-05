# Changelog

## 1.1.0

Content compatibility and naming polish release for Minecraft `1.21.1` on
Fabric and NeoForge.

### Highlights

- Added built-in optional content compat packs.
- Added cross-loader compat for The Aether, Deeper and Darker, and Bosses of
  Mass Destruction.
- Added NeoForge compat for L_Ender's Cataclysm and The Twilight Forest.
- Added Fabric compat for BetterEnd, BetterNether, AdventureZ, VoidZ, and
  Mythic Metals.
- Added cross-loader worldgen/content compat for Terralith, Incendium, Dungeons
  and Taverns, When Dungeons Arise, Towns and Towers, Regions Unexplored,
  Explorify, and Illager Invasion.
- Improved English and Russian generated-name text for vanilla and compat
  names.
- Fixed fall-death naming variety so repeated fall-death places are not all
  named `Последний Уступ` or the same single name.
- Improved naming audits for fixed-pattern pools.
- Kept JourneyMap, Xaero's Minimap, and FTB Chunks behavior unchanged.
- Build and runtime smoke passed on Fabric and NeoForge.

### Known Limitations

- Fabric and NeoForge are both targeted at Minecraft `1.21.1`.
- Map integrations are optional and require the matching client map mod.
- Xaero and FTB Chunks compatibility is destination-only.
- Some content compat is loader-specific.
- Existing named places do not automatically rename themselves after a naming
  update.
- Balance may still change in future updates.

## 1.0.0

First public 1.0.0 release for World Remembers: Living Legends.

### Highlights

- Important world events can become named places.
- Fabric `1.21.1` support.
- NeoForge `1.21.1` support.
- Six generated naming styles.
- World Journal item and GUI on both supported loaders.
- Place title overlay on both supported loaders.
- `/places` commands, storage, events, networking, and compat registry support
  on both supported loaders.
- Candidate decay for unpromoted activity.
- General landmark spacing.
- Deleted-place suppression.
- Import/export.
- Config validation.
- Save-data migrations.
- Self-test, validate, and repair dry-run commands.
- Data-driven compat registry foundation.
- JourneyMap integration with discovered fantasy place labels, themed label
  styling, clean tooltips, and World Journal destinations.
- Xaero compatibility with manual World Journal destination waypoints.
- FTB Chunks compatibility with manual World Journal destination waypoints.
- Portal places now anchor on the source side of the portal trip.
- Stability and performance cleanup for optional map integrations.
- MIT License.

### Known Limitations

- Fabric and NeoForge are both targeted at Minecraft `1.21.1`.
- Map integrations are optional and require the matching client map mod.
- Xaero and FTB Chunks compatibility is destination-only.
- Broad third-party content compat was expanded in later releases.
- Balance may still change in future updates.
