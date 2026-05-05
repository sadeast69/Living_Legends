# GitHub Upload Checklist

Use this when preparing the public `v1.1.0` source and release artifacts.

## 1. Review Repository State

- Make sure source files, docs, Gradle files, and resources are present.
- Make sure local folders such as `.gradle`, `build`, `run`, `logs`, `config`,
  and `saves` are ignored or excluded.
- Do not include built jars in the source tree.
- Review the repository state before publishing.

## 2. Build

Run a clean build before publishing.

Expected loader jars:

```text
fabric/build/libs/living_legends-fabric-1.1.0.jar
neoforge/build/libs/living_legends-neoforge-1.1.0.jar
```

## 3. Test In Minecraft

In a test world, run:

```mcfunction
/places debug selftest
/places debug validate
/places debug compat summary
```

Open the World Journal and check that the mod starts normally.

## 4. Prepare Public Source Archive

Expected source archive:

```text
world-remembers-living-legends-1.1.0-github-source-clean.zip
```

The source archive should include public source, docs, Gradle wrapper files,
license, README, and changelog. It should not include build outputs, run
folders, logs, generated jars, private notes, local paths, review packages, or
target mod jars/classes.

## 5. Create The GitHub Repository Or Release

Create or update the GitHub repository as needed. If creating a release, use
tag:

```text
v1.1.0
```

Use `docs/RELEASE_PAGE_DRAFT.md` as the starting release text.

## 6. Upload Release Assets

Upload these release assets:

```text
living_legends-fabric-1.1.0.jar
living_legends-neoforge-1.1.0.jar
world-remembers-living-legends-1.1.0-github-source-clean.zip
```

Do not commit generated jars to source control.

## 7. After Upload

- Download each release asset once and compare file names and versions.
- Check that README says Fabric `1.21.1` and NeoForge `1.21.1`.
- Check that the release page mentions version `1.1.0`.
- Check that the source archive opens with normal `/` path separators.
- Check that no private notes, local paths, generated jars, logs, or target mod
  jars/classes are present in the source archive.

