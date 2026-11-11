# Test suite

Run commands from the repository root (`gradlew.bat` on Windows, `./gradlew` elsewhere):

```sh
./gradlew runGameTestServer
./gradlew runClient -Pclient_smoke_test=true
./gradlew runGameTestServer -Pinclude_create=false -Pinclude_cookery=false -Pinclude_farmers_delight=false
```

`build` compiles the mod; it does not run the GameTest or client suites. CI runs server tests against both supported Cookery versions, with and without Create. Client tests require a graphical environment and exit after checking normal resources, a replacement resource pack, and restoration of the original resources.

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

Restore modified configuration, rules, listeners, entities and client render state. Keep fixtures within their arena; when a test deliberately uses remote positions, remove its blocks and entities. Preserve save migration tests while the corresponding compatibility behavior remains supported.

Avoid exact registry totals, copied ordering lists, individual artwork pixels and assertions that only prove an obsolete recipe remains deleted. Check family relationships, visible behavior and resource-pack compatibility instead. Geometry checks should protect interaction or alignment constraints, not freeze an entire model.

## Rendering diagnostics

Default client tests do not write preview files. Pixel readback remains enabled where it supports an assertion, such as lighting or invisible geometry. To save diagnostic images from the remaining rendering checks:

```sh
./gradlew runClient -Pclient_smoke_test=true -Ptest_images=true
```

Images are written to `run/test-artifacts/`. They assist manual inspection and are not golden-image snapshots or substitutes for assertions. Resource-pack fixture files are created independently of this option because reload tests need them.
