# Release Notes

## 1.0.0

This is the first public 1.0.0 release for World Remembers: Living Legends.

Back up important worlds before testing. The mod has save-data migrations and
validation, and backups are still the safest way to protect long-running worlds
before installing or updating any mod.

## Highlights

- World memory events can become saved named places.
- Generated place names with six built-in styles.
- Fabric `1.21.1` support.
- NeoForge `1.21.1` support.
- World Journal item and book-style GUI on both supported loaders.
- Place title overlay using server-side place entry detection on both supported
  loaders.
- `/places` commands, storage, events, networking, and compat registry support
  on both supported loaders.
- Candidate decay for stale unpromoted activity.
- General landmark spacing to reduce map clutter.
- Deleted-place suppression after manual deletion.
- Import/export commands.
- Config validation and clamping.
- Save-data schema versioning and migration.
- Debug self-test, validate, and repair dry-run commands.
- Data-driven compat registry foundation.
- Optional ModMenu integration on Fabric.
- MIT License.

## Current Support

- Minecraft `1.21.1`
- Fabric `1.21.1`
- NeoForge `1.21.1`
- Java 21
- Fabric API required for Fabric builds
- ModMenu optional on Fabric

## Notes For Existing Worlds

Saved data is versioned and migrated on load. Existing place names are not
renamed automatically.

Recommended before testing:

```mcfunction
/places debug selftest
/places debug validate
/places export
```

## Known Limitations

- Fabric and NeoForge are both targeted at Minecraft `1.21.1`.
- No map or waypoint integration yet.
- Third-party compat packs are not bundled yet.
- Balance may change in future updates.
- Journal art and small UI details may still be refined later.
