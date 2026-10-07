# Changelog

## 1.1.0 - 2026-10-07

### Added
- **Gloss Tiles (experimental)**: glossy floor tiles in all 16 dye colours that show a mirror image of players and mobs standing on them, without shaders. The reflection uses a stencil mask plus a mirrored entity pass and a glaze layer. This feature is new and has not been fully verified in every setup yet, so please report anything that looks wrong.
  - Crafted as 8 tiles from 4 plain quartz blocks, 4 glass panes and 1 dye.
  - Config: `glossReflections` (on/off), `glossStrength` (0 = plain tile, 1 = perfect mirror, default 0.45), `glossRange` (4-64 blocks, default 24, fades out over the last third), `glossPlayerOnly` (performance mode: only you are reflected).
  - Reflects players, mobs, dropped items and projectiles. Creatures that are off-screen and tiles seen from below are skipped to save frame time.
  - `/pfgloss <0-3>` (op) debug views: normal, mirror without clipping, show the tile mask, mask without depth test.

### Fixed
- Gloss tile recipes only accept the plain quartz block (not chiseled or pillar quartz).
- Reflections no longer disappear under Celeritas (the vanilla frustum test was stale there and hid every creature).

