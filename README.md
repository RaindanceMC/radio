# RaindanceMC/radio

- **`radio-module`** — Rain's Flexible Modules — Radio: `/music` gives every player their own radio with four stations (Calm, Adventure, Cozy, Night). Each station plays its own tracks shuffled, heard only by that player, on the Jukebox/Note Blocks slider. Skip, stop, volume, a "now playing" line on the action bar. Whether it is on, the station and the volume are kept on the player, so the radio comes back by itself after logging on again.
- **`radio-standalone`** — Rain's Radio: the module and the standard wrapper, shaded together.

Tracks: every `.ogg` (Vorbis) in `plugins/RainsRadio/music/<calm|adventure|cozy|night>/`, offered to the server's resource pack through Core (`content.music.MusicPack`, shelves). The file name is the title shown, e.g. `Beyond The Clouds - Matthew Pablo.ogg`. New tracks come with the next restart.

Who plays music when several things want to is Core's `content.music.MusicStage` (2.115.0): music on logging on < AFK camera < radio < one-off moments (first visit of a dimension). The radio silences the join and AFK music; a moment pauses the radio, which carries on afterwards.

`/music` opens the page; `/music station <name>` (or just `/music night`), `skip`, `stop`, `on`, `volume <5-100>`, `status`. Alias `/radio`. Settings page "Radio".

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework).

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `radio-standalone/target/RainsRadio-0.2.0.jar` — needs RainsCore 2.115.0 or newer

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.

End to end: `RadioScenarioTest` in [e2e](https://github.com/RaindanceMC/e2e).
