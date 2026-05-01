# Contributing

Thanks for wanting to help with World Remembers: Living Legends.

The project is currently Fabric `1.21.1` first. Ports, datapack compat, small
fixes, bug reports, and pull requests are welcome.

## Before A Large Change

Please open an issue before:

- large rewrites
- new gameplay systems
- save-data schema changes
- networking changes
- loader ports
- big balance changes

Small bug fixes and docs improvements can go straight to a pull request.

## Building

Run:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

Do not submit generated build folders, run folders, logs, or local config files.

## Bug Reports

Please include:

- Minecraft version
- loader and loader version
- World Remembers version
- Fabric API version
- other relevant mods
- `latest.log` or crash report
- steps to reproduce
- what you expected
- what actually happened

## Pull Requests

In your PR, explain:

- what changed
- why it changed
- how you tested it
- whether docs or config changed
- whether save data or schema changed

Keep changes focused. A small PR is much easier to review than a heroic one.

## Compat Work

Simple compat can often be a datapack. See [docs/COMPAT_API.md](docs/COMPAT_API.md)
before adding Java-side integration.
