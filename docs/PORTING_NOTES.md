# Porting Notes

These notes are for contributors who want to port World Remembers to another
loader or Minecraft version, or keep existing loader implementations in parity.

Current public targets:

- Fabric `1.21.1`
- NeoForge `1.21.1`
- Java 21
- Yarn mappings
- Mojmap/NeoForge mappings

Fabric is the gameplay reference implementation. The NeoForge module is a
supported `1.21.1` loader implementation and should remain behavior-compatible
with Fabric unless a loader-specific API requires a small adaptation.

## What Is Portable

Most core logic lives outside Fabric-specific classes:

- place types and clustering
- scoring and thresholds
- naming and NameRecipe resolution
- candidate decay
- deleted-place suppression
- config model and validation
- compat registry definitions
- schema migration helpers

Keep these pieces as close to loader-neutral as possible.

## What Is Platform-Specific

Expect real work in these areas:

- command registration and permissions
- event hooks
- saved data / PersistentState equivalent
- networking payload registration
- client title overlay rendering
- World Journal screen and item opening
- optional ModMenu integration
- resource reload listeners for compat registries

## Storage

The Fabric storage/NBT layer now uses typed Minecraft classes instead of the old
reflection-heavy bridge. That should make future porting safer, but it does not
make a port automatic.

When porting storage:

- keep save ids stable
- keep NBT keys stable
- preserve schema version fields
- run migration and validation after loading
- never silently replace old data with an empty state

## Compat Registries

Compat registries are data-driven. This is the easiest area to keep portable:
load the same datapack JSON folders and feed the same definition objects into
the resolver.

## Suggested Porting Order For New Loader Ports

1. Storage.
2. Commands.
3. Events.
4. Networking.
5. Client UI.
6. Compat registry reload integration.

Do not start with the journal or overlay. They are visible and tempting, but
they depend on storage, networking, and server-side place logic already working.

## Testing A Port

Minimum checks:

```mcfunction
/places debug selftest
/places debug validate
/places list
/places nearest
/places info nearest
```

Then test:

- create, rename, delete
- save and reload world
- export and import
- World Journal open
- title overlay packet and render
- compat summary and lookup
