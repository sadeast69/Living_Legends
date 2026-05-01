# Commands

World Remembers uses `/places` for place lookup, admin tools, and diagnostics.
Normal players can inspect known places. Management and debug commands are for
operators unless your server config says otherwise.

## Player Commands

### `/places list`

Who: players.

Shows a short list of active named places.

Example:

```mcfunction
/places list
```

Useful when you want to see what the world currently remembers.

### `/places nearest`

Who: players.

Shows the nearest saved place to you.

Example:

```mcfunction
/places nearest
```

Useful after exploring or when a title overlay appeared and you want details.

### `/places info <id|nearest|here>`

Who: players.

Shows details for a place. `nearest` uses the closest place. `here` checks the
place containing your current position.

Examples:

```mcfunction
/places info nearest
/places info here
/places info #1
```

Useful for coordinates, type, and the current saved name.

## OP / Admin Commands

### `/places create <type> <radius> <name>`

Who: OP/admin.

Creates a named place manually at your position.

Example:

```mcfunction
/places create GENERAL_LANDMARK 32 Old Road
```

Useful for server spawn, community builds, or fixing a missing place.

### `/places rename <id|nearest|here> <new_name>`

Who: OP/admin. The World Journal may also allow this for singleplayer owners or
configured servers.

Sets a manual name for a place.

Example:

```mcfunction
/places rename nearest Ashen Rest
```

Useful when a generated name is close but not quite right.

### `/places delete <id|nearest|here>`

Who: OP/admin.

Deletes an active place and creates a suppression marker so old activity does
not recreate it immediately.

Example:

```mcfunction
/places delete nearest
```

Useful for removing noisy or unwanted places.

### `/places regenerate <id|nearest|here>`

Who: OP/admin.

Regenerates a place name from its saved naming data where possible.

Example:

```mcfunction
/places regenerate nearest
```

Useful after changing naming style settings or restoring a generated name.

### `/places export`

Who: OP/admin.

Exports saved place data to the configured export location.

Example:

```mcfunction
/places export
```

Useful before risky tests, migrations, or server maintenance.

### `/places import`

Who: OP/admin.

Imports place data from the configured import location. Older exports without
version fields are migrated before insertion.

Example:

```mcfunction
/places import
```

Useful for moving places between test worlds or recovering from a backup.

### `/places reload`

Who: OP/admin.

Reloads World Remembers config and runs config validation.

Example:

```mcfunction
/places reload
```

Useful after editing config files.

## Debug / Admin Commands

Debug commands are meant for troubleshooting. They are not normal gameplay UI.

### `/places debug selftest`

Who: OP/admin.

Runs a fast non-destructive health check: config, data, naming, networking, and
core systems.

Example:

```mcfunction
/places debug selftest
```

Useful before reporting a bug.

### `/places debug validate`

Who: OP/admin.

Scans current saved data for broken place records, invalid radii, missing names,
or orphan journal references.

Example:

```mcfunction
/places debug validate
```

Useful after imports, migrations, or heavy testing.

### `/places debug repair dryrun`

Who: OP/admin.

Reports what a repair pass would fix without changing world data.

Example:

```mcfunction
/places debug repair dryrun
```

Useful when validate reports warnings and you want to inspect them first.

### `/places debug config validate`

Who: OP/admin.

Shows the latest config validation result.

Example:

```mcfunction
/places debug config validate
```

Useful after intentionally testing bad config values.

### `/places debug chunk`

Who: OP/admin.

Shows memory stats for your current chunk.

Example:

```mcfunction
/places debug chunk
```

Useful when a place is not generating where expected.

### `/places debug nearby`

Who: OP/admin.

Shows nearby saved places and candidate context around your position.

Example:

```mcfunction
/places debug nearby
```

Useful for duplicate spacing and overlap checks.

### `/places debug farm`

Who: OP/admin.

Shows anti-farm state and counters.

Example:

```mcfunction
/places debug farm
```

Useful when combat or slaughter events are being rejected.

### `/places debug namebatch`

Who: OP/admin.

Generates a small batch of test names without creating places.

Example:

```mcfunction
/places debug namebatch DEATH_SITE SURFACE vanilla_adventure 10
```

Useful when checking naming style variety.

### `/places debug namecause`

Who: OP/admin.

Generates test names for a specific place type, cause, target id, and style.

Example:

```mcfunction
/places debug namecause PET_MEMORIAL PET_DEATH minecraft:wolf cozy_survival 10 name=Барсик
```

Useful for pet, named mob, boss, and compat naming tests.

### `/places debug nameaudit`

Who: OP/admin.

Runs a compact naming audit for style/context coverage.

Example:

```mcfunction
/places debug nameaudit
```

Useful before release or after adding naming data.

### `/places debug decay`

Who: OP/admin.

Inspects candidate decay around the current chunk or nearest candidate.

Example:

```mcfunction
/places debug decay nearest
```

Useful when stale unpromoted activity is not fading as expected.

### `/places debug spacing`

Who: OP/admin.

Explains place spacing decisions for your current position and type.

Example:

```mcfunction
/places debug spacing here GENERAL_LANDMARK
```

Useful for checking why a generic landmark was suppressed.

### `/places debug title`

Who: OP/admin.

Tests or inspects the place title overlay.

Examples:

```mcfunction
/places debug title test vanilla_adventure GENERAL_LANDMARK "Old Road"
/places debug title nearest
/places debug title here
/places debug title clear
```

Useful when checking title timing, priority, and client rendering.

### `/places debug compat`

Who: OP/admin.

Shows or looks up data-driven compat registry entries.

Examples:

```mcfunction
/places debug compat summary
/places debug compat lookup boss minecraft:wither
/places debug compat lookup mob minecraft:creeper
/places debug compat lookup block minecraft:diamond_ore
/places debug compat lookup dimension minecraft:the_nether
```

Useful when testing datapack overrides or modded ids.
