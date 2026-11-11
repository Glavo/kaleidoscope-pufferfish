# Test suite

Run commands from the repository root (`gradlew.bat` on Windows, `./gradlew` elsewhere):

```sh
./gradlew runGameTestServer
./gradlew runClient -Pclient_smoke_test=true
./gradlew runGameTestServer -Pinclude_create=false -Pinclude_cookery=false -Pinclude_farmers_delight=false
```

`build` compiles the mod; it does not run the GameTest or client suites. CI runs server tests against both supported Cookery versions, with and without Create. Client tests require a graphical environment and exit after checking normal resources, a replacement resource pack, and restoration of the original resources.

Server tests use `build/test-runs/server`; client smoke tests use `build/test-runs/client`. These runs are isolated from development saves, configuration and manually installed mods in `run/`. No init script is needed. Resource-pack fixtures are reused under the client test directory and removed by `clean`.

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
