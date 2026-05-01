# Release Notes

## 1.0.0-rc.1

This is the first release-candidate packaging pass for World Remembers: Living
Legends.

Back up important worlds before testing. The mod has save-data migrations and
validation, but this is still an RC build.

## Highlights

- World memory events can become saved named places.
- Generated place names with six built-in styles.
- World Journal / Журнал Мира item and book-style GUI.
- Place title overlay inspired by Traveler's Titles, using server-side place
  entry detection.
- Candidate decay for stale unpromoted activity.
- General landmark spacing to reduce map clutter.
- Deleted-place suppression after manual deletion.
- Import/export commands.
- Config validation and clamping.
- Save-data schema versioning and migration.
- Debug self-test, validate, and repair dry-run commands.
- Data-driven compat registry foundation.
- Optional ModMenu integration.
- MIT License.

## Current Support

- Fabric `1.21.1`
- Java 21
- Fabric API required
- ModMenu optional

NeoForge is not released as supported gameplay content yet. The module is a
placeholder for future work.

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

- Fabric `1.21.1` only for this RC.
- No supported NeoForge release yet.
- No map or waypoint integration yet.
- Third-party compat packs are not bundled yet.
- Balance may change during RC playtesting.
- Journal art and small UI details may still be refined later.
