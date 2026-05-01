# Config

World Remembers writes config files under:

```text
config/world_remembers/living_legends/
```

The mod validates config on load and on `/places reload`. Bad values are clamped
or replaced with safe defaults where possible. Check `latest.log` if validation
reports warnings.

## Main Sections

### Generation

Controls how memory events turn into place candidates and then named places.

Important knobs:

- cluster radius
- candidate cooldowns
- place radius defaults
- duplicate spacing
- enabled place types

Raise values if the world feels noisy. Lower them if places feel too rare.

### Thresholds

Thresholds decide how much local activity is needed before a candidate can
become a named place.

Common fields:

- `scoreThreshold`
- `requiredRawCount`
- per-event weights

Example: lowering the BATTLEFIELD threshold makes combat sites appear sooner.
Do not set thresholds to zero on a public server unless you want a very busy
map.

### Enabled / Disabled Place Types

Use this if you want to turn off a whole category.

Example use cases:

- disable `GENERAL_LANDMARK` for a very quiet server
- disable `SLAUGHTER_FIELD` if animal-farm names do not fit the server
- keep memorial types enabled but make combat sites rarer

### Naming

Controls the default generated-name style.

Built-in styles:

- `vanilla_adventure`
- `neutral_server`
- `dark_fantasy`
- `cozy_survival`
- `epic_mythology`
- `funny_community`

If the configured style is unknown, validation falls back to
`vanilla_adventure`.

### Title Overlay

Controls the title shown when a player enters a saved place.

Useful fields:

- `enabled`
- `checkIntervalTicks`
- `globalCooldownTicks`
- `samePlaceCooldownTicks`
- `generalLandmarkCooldownTicks`
- `teleportDelayTicks`
- `showGeneralLandmarks`
- `generalLandmarkOnlyIfNoHigherPriority`

To reduce title spam, increase cooldowns or disable general landmark titles.
The server is the source of truth; the client only renders the title it receives.

### Journal

Controls the World Journal item and GUI.

Visibility modes:

- `ALL_KNOWN`: players can see all active places in the world.
- `VISITED_BY_PLAYER`: players only see places they have discovered.

Management modes:

- `SINGLEPLAYER_OWNER_AND_OP`
- `OP_ONLY`
- `ALL_PLAYERS`
- `DISABLED`

`pageSize` controls how many places the server returns per journal request. The
book UI may show fewer entries per visible page for readability.

### Candidate Decay

Candidate decay affects only unpromoted candidate activity.

It does not delete or weaken existing `NamedPlace` records.

Use this to keep old almost-places from building up forever. If decay feels too
aggressive, increase the grace period or lower decay per interval.

### General Landmark Spacing

`GENERAL_LANDMARK` is background content. Without spacing, normal movement can
turn the map into a pile of generic walking spots.

Spacing options keep these landmarks sparse:

- same-type minimum distance
- any-place minimum distance
- merge distance
- suppression cooldown

To make generic landmarks rarer, increase the same-type and any-place distances.

### Notifications

Controls server messages about place creation or related events.

Keep notification radius and cooldowns sensible on busy servers.

### Debug

Debug options are for testing. Keep them off for normal gameplay.

`debug.namingVerbose` can produce a lot of logs because it explains rejected
name patterns. Only enable it while investigating naming issues.

### Compat

Compat registries are loaded from datapack JSON and built-in data. They provide
theme hints for bosses, structures, biomes, mobs, valuable blocks, and
dimensions.

See [COMPAT_API.md](COMPAT_API.md).

## Practical Tuning

### Make places appear more often

- Lower the relevant `scoreThreshold`.
- Lower `requiredRawCount`.
- Reduce candidate cooldowns.
- Keep anti-farm enabled so obvious farms still get filtered.

### Make places rarer

- Raise score thresholds.
- Raise required raw counts.
- Increase duplicate spacing.
- Increase general landmark spacing.

### Make battlefields easier or harder

Look for the BATTLEFIELD score threshold, required raw count, and hostile/neutral
kill weights.

- Easier: lower threshold or raw count a little.
- Harder: raise threshold or raw count.

Do not disable anti-farm just to make battlefields easier. Tune the battlefield
numbers first.

### Reduce title overlay spam

- Increase `globalCooldownTicks`.
- Increase `samePlaceCooldownTicks`.
- Increase `generalLandmarkCooldownTicks`.
- Set `showGeneralLandmarks` to false if generic titles feel noisy.

### Disable a place type

Use the place type enable/disable section and add the type you do not want, such
as `SLAUGHTER_FIELD` or `GENERAL_LANDMARK`.

After editing config:

```mcfunction
/places reload
/places debug config validate
```
