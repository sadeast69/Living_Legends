# Compat API And Datapack Registries

World Remembers can read small JSON files from datapacks to understand modded
bosses, structures, biomes, mobs, valuable blocks, and dimensions.

Simple compat does not need a Java dependency. A datapack can add a new id or
override a vanilla mapping.

This system is new in `1.0.0-rc.1` and may grow during RC testing. Keep compat
packs simple and report issues.

## Datapack Paths

Use these folders inside a datapack:

```text
data/living_legends/world_remembers/boss_themes/*.json
data/living_legends/world_remembers/structure_themes/*.json
data/living_legends/world_remembers/biome_themes/*.json
data/living_legends/world_remembers/mob_themes/*.json
data/living_legends/world_remembers/block_themes/*.json
data/living_legends/world_remembers/dimension_themes/*.json
```

## Priority

When more than one entry matches, the highest `priority` wins.

At the same priority, exact ids beat broad tag matches. Datapack entries can
override built-in vanilla entries by using a higher priority, such as `100` or
`999`.

Unknown mod ids should not crash the server. If the mod is not installed yet,
the entry may show as unresolved in debug lookup. Once the mod is installed, the
same JSON can start matching without code changes.

## Boss Theme

```json
{
  "entity": "examplemod:giant_king",
  "bossTheme": "giant_king",
  "placeType": "BOSS_SITE",
  "causeType": "BOSS_KILL",
  "priority": 100,
  "enabled": true,
  "tags": ["examplemod"]
}
```

Use this for modded bosses that should create boss-site naming context.

## Structure Theme

```json
{
  "structure": "examplemod:ancient_tower",
  "structureTheme": "ancient_tower",
  "placeType": "FIRST_DISCOVERY",
  "discoveryKind": "structure",
  "priority": 100,
  "useStructureBounds": true,
  "enabled": true
}
```

Use this for structures that should get better first-discovery names.

## Biome Theme

Exact biome:

```json
{
  "biome": "examplemod:crystal_forest",
  "biomeTheme": "crystal_forest",
  "biomeGroup": "forest",
  "priority": 100,
  "enabled": true
}
```

Biome tag:

```json
{
  "biomeTag": "c:is_cave",
  "biomeTheme": "cave",
  "biomeGroup": "underground",
  "priority": 10,
  "enabled": true
}
```

Use exact ids for special biomes and tags for broad families.

## Mob Theme

```json
{
  "entity": "examplemod:ash_skeleton",
  "mobTheme": "ash_skeleton",
  "mobGroup": "undead",
  "combatRole": "hostile",
  "placeType": "BATTLEFIELD",
  "priority": 100,
  "enabled": true
}
```

`combatRole` can be `hostile`, `neutral`, `passive`, `boss`, `companion`, or
`unknown`.

## Block Theme

```json
{
  "block": "examplemod:moonstone_ore",
  "blockTheme": "moonstone",
  "miningTheme": "moonstone",
  "valuable": true,
  "firstDiscoveryKey": "moonstone",
  "priority": 100,
  "enabled": true
}
```

Use this for valuable ores and blocks that should influence mining-site or
first-discovery names.

## Dimension Theme

```json
{
  "dimension": "examplemod:sky_realm",
  "dimensionTheme": "sky_realm",
  "portalTheme": "sky_portal",
  "firstDiscoveryKey": "sky_realm",
  "priority": 100,
  "enabled": true
}
```

Use this for portal landmarks, dimension thresholds, and dimension-discovery
names.

## Debug Commands

```mcfunction
/places debug compat summary
/places debug compat lookup boss minecraft:wither
/places debug compat lookup mob minecraft:creeper
/places debug compat lookup block minecraft:diamond_ore
/places debug compat lookup dimension minecraft:the_nether
```

Use these after `/reload` to confirm that a datapack entry is loaded and winning
the priority check you expect.

## Internal Java API

The mod also has an internal compat API for future modules. It is not promised
as stable public API yet. Prefer datapack JSON unless you need Java-side logic.
