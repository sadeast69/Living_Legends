# Pre-Release Checklist

Use this before tagging a public release.

## Build

- [ ] Run a clean full build.
- [ ] Start the Fabric client.
- [ ] Start the NeoForge client.
- [ ] Start an integrated server.
- [ ] Optional: start a dedicated Fabric server.
- [ ] Optional: start a dedicated NeoForge server.
- [ ] Confirm Fabric and NeoForge release jars contain only their expected
  loader metadata and resources.

## Data

- [ ] Run `/places debug selftest`.
- [ ] Run `/places debug validate`.
- [ ] Create a place.
- [ ] Rename a place.
- [ ] Delete a place.
- [ ] Save and reload the world.
- [ ] Confirm the created/renamed/deleted state persists.
- [ ] Run `/places export`.
- [ ] Run `/places import` in a test world.

## Gameplay

- [ ] Player visit can contribute to place memory.
- [ ] Player death can create or candidate a death site.
- [ ] Fall-death sites show generated-name variety across new locations.
- [ ] Hostile kills can create a battlefield after enough real combat.
- [ ] Passive kills can create a slaughter field only after enough activity.
- [ ] Named pet death creates a pet memorial.
- [ ] Named non-pet mob death creates a named mob memorial.
- [ ] Portal usage contributes to portal landmarks.
- [ ] First discovery events still work.
- [ ] World Journal opens from the item.
- [ ] World Journal search/filter/sort/page controls work.
- [ ] World Journal rename/delete/restore permissions work.
- [ ] Place title overlay triggers on entry.
- [ ] Place title overlay fades out without flicker.

## Config

- [ ] Default config loads without warnings.
- [ ] Bad numeric values are clamped or reset.
- [ ] Bad enum values fall back safely.
- [ ] `/places reload` validates config.
- [ ] `/places debug config validate` reports the latest validation result.

## Compat

- [ ] Run `/places debug compat summary`.
- [ ] Lookup vanilla boss: `/places debug compat lookup boss minecraft:wither`.
- [ ] Lookup vanilla mob: `/places debug compat lookup mob minecraft:creeper`.
- [ ] Lookup vanilla block: `/places debug compat lookup block minecraft:diamond_ore`.
- [ ] Lookup vanilla dimension: `/places debug compat lookup dimension minecraft:the_nether`.
- [ ] Lookup at least one built-in cross-loader content compat id.
- [ ] Confirm Fabric-only compat JSON is absent from the NeoForge jar.
- [ ] Confirm NeoForge-specific Cataclysm/Twilight Forest compat JSON is absent
  from the Fabric jar.
- [ ] Optional: test a datapack override in a disposable world.

## Map Integrations

- [ ] JourneyMap discovered labels appear when JourneyMap is installed.
- [ ] JourneyMap World Journal destination waypoint works.
- [ ] Xaero World Journal destination waypoint works when Xaero is installed.
- [ ] FTB Chunks World Journal destination waypoint works when FTB Chunks is
  installed.

## Release Artifacts

- [ ] Jars do not contain temporary test datapacks.
- [ ] Jars do not contain logs, local configs, or test worlds.
- [ ] Jars do not contain target mod jars/classes.
- [ ] Public source archive excludes generated jars, build folders, logs,
  private notes, local paths, review packages, and target mod jars/classes.
- [ ] README lists Fabric and NeoForge `1.21.1` support.
- [ ] ModMenu remains optional.
- [ ] MIT License is present.
- [ ] No obvious normal-gameplay log spam.

