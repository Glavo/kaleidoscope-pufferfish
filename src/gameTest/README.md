# Test suite

Run commands from the repository root (`gradlew.bat` on Windows, `./gradlew` elsewhere):

```sh
./gradlew runGameTestServer
./gradlew runClient -Pclient_smoke_test=true
./gradlew runGameTestServer -Pinclude_create=false -Pinclude_cookery=false -Pinclude_farmers_delight=false
```

`build` compiles the mod; it does not run the GameTest or client suites. CI runs server tests against both supported Cookery versions, with and without Create. Client tests require a graphical environment and exit after checking normal resources, a replacement resource pack, and restoration of the original resources.

Server tests use `build/test-runs/server`; client smoke tests use `build/test-runs/client`. These runs are isolated from development saves, configuration and manually installed mods in `run/`. No init script is needed. Resource-pack fixtures are reused under the client test directory and removed by `clean`.

## Manual rank testing

With operator permission level 2, hold a single pufferfish, bucket or variant-derived item and run:

```mcfunction
/pufferfish level set @s 4
/pufferfish experience set @s 4000
/pufferfish level query @s
/pufferfish experience query @s
/pufferfish level set @e[type=minecraft:pufferfish,distance=..16] 5
```

Commands follow `<property> set <targets> <value>` and `<property> query <target>`. Targets are required and accept player names, entity UUIDs and vanilla selectors, including execution from the console or command blocks. A player target edits the main-hand item; a live pufferfish target edits its own data and refreshes its size. Other entities are rejected. `set` accepts multiple targets, while `query` requires exactly one. Properties are `level` (0–10, with rank-name completion hints) and `experience` (a nonnegative long). Queries do not change item or entity data; their command result is the numeric value, capped at `Integer.MAX_VALUE` for experience.

Each edit changes only the selected property, preserving talent and lifetime experience. Newly awakened items start with talent 0.5 and zero experience. Setting level zero removes cultivation data; setting experience requires an awakened item. Fish weapons are activated when setting their level, even at zero. Edits bypass breakthrough requirements without granting advancement rewards or immediately advancing an experience-complete rank.

Generic cutting boards only produce sashimi; pressing produces unranked skins. Skins only store `skin_level`: processing table recipes inherit rank without talent, experience, lifetime totals, readiness or stars. Same-variant skins of the same rank stack normally; rank zero has no extra component. Placement stores rank in block state, retaining it through pick-block, drops and copper transformations. Skin commands support `level` but reject `experience`.

Player targets must hold exactly one item; no items are split, moved or dropped. Every target is validated before any item or entity changes. Buckets edit their contained fish, preserving captured entity data. Lamps, skins, armor, shields, boats and flails use their item component; spawn eggs are excluded. Other derivative crafting recipes do not automatically transfer cultivation.

## Pufferfish stand

Insert one fish or skin, cut with a knife or shears, brush to lower rank, or retrieve with an empty hand. Three brush strokes remove one rank and clear current XP; talent and lifetime XP remain until rank zero. Rank zero clears cultivation. Full, unenchanted fish with mod-managed durability can then return to their original stack limit; wear, enchantments and foreign durability are never erased. Taking the item out clears partial processing progress.

Completed fish downgrades refund one quarter of current XP plus the talent-adjusted cost of reaching the old rank, rounded down. Awakening has no XP cost; skins and rank-zero restoration refund nothing. Refunds use vanilla merged experience orbs, including normal pickup and Mending behavior; lifetime XP and star counts are never counted separately.

Recipes use `pufferfish_please:fish_processing` with `ingredient`, `tool`, `strokes`, `results` and optional `inherit_skin_level`. The table accepts other ingredients when a data pack supplies a recipe. No idle block entity ticker is required. Client smoke tests save a rendered preview to `build/test-runs/client/screenshots/fish-processing-table.png`.

## Iron golem headgear

Sneak-right-click an iron golem with a pufferfish to add one to its tower, up to 256 mixed fish. Sneak-right-click with an empty hand to retrieve the top fish; Binding Curse prevents survival removal of that fish. Ordinary right-clicks remain available for throwing. Each fish retains its components through save/load and tracking. Only the bottom fish occupies the head slot and provides effects or takes wear; the next fish replaces it when it breaks. Death drops obey mob loot and Vanishing Curse. Iron ingots still repair the golem. Towers do not change collision bounds. Client tests cover mixed towers, head transforms, effect limits and visibility above an off-screen golem.

## Live fish health

Ranks 0–10 multiply maximum health by 1, 1.2, 1.5, 2, 2.7, 3.6, 4.8, 6.2, 7.5, 8.7 and 10. Stars do not add health. Rank changes preserve the current health percentage; world and bucket reloads preserve current health. The modifier composes with external health bonuses and respects the native 1024 maximum. Capture limits remain proportional to maximum health.

## Ownership

- `*GameTests`: observable server behavior, real interactions, persistence, synchronization, event cancellation and item conservation.
- `PufferfishVariantGameTests`: family completeness and variant identity. Other suites exercise those families instead of repeating registration counts.
- `PufferfishEnchantmentGameTests`: book acceptance, upgrades and costs. Loyalty and other specialized suites cover their runtime effects.
- `PufferfishResourcesClientTest`: models, sprites, translations and resource selection. Domain client tests cover rendering behavior, animation and state restoration.
- `compat/`: integrations registered only when the corresponding mod is present.
- `test/`: reusable fixtures and assertions. Test holders are not utility libraries.

## Adding coverage

Give each behavior one primary owner. Use variant matrices where behavior is shared, and separate tests where entry points or failure modes differ. Keep independent expected results; do not call production code to compute the same value being asserted.

Prefer real item use, collisions and menus over invoking an event handler directly. Isolated effect and event tests are still useful for cancellation and boundary conditions. Use fixed random sources or explicit boundary rolls, not unseeded frequency assertions.

Restore modified configuration, rules, listeners, entities and client render state. Keep fixtures within their arena; when a test deliberately uses remote positions, remove its blocks and entities. Test current save/load behavior rather than historical formats that the mod no longer supports.

Avoid exact registry totals, copied ordering lists, individual artwork pixels and assertions that only prove an obsolete recipe remains deleted. Check family relationships, visible behavior and resource-pack compatibility instead. Geometry checks should protect interaction or alignment constraints, not freeze an entire model.

Rendering tests must assert observable output. Prefer captured vertices, textures and restored transforms; use framebuffer readback only for pixel-dependent behavior such as transparency and lighting. Do not keep preview galleries, image-export switches or standalone probes outside the normal test entry points. Reflection is limited to framework fixtures or otherwise inaccessible vanilla entry points; do not invoke private mod helpers to mirror their implementation.
