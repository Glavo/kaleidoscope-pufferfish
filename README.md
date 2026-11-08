# Kaleidoscope Pufferfish

Dye live pufferfish, pufferfish items, and pufferfish buckets in all 16 Minecraft
colors. Targets Minecraft 1.21.1 and NeoForge with Java 21.

## Usage

- Use a dye on a live pufferfish. Survival mode consumes one dye when the color
  changes; creative mode consumes none. Dyed fish do not naturally despawn.
- Craft one pufferfish or pufferfish bucket with one dye in any crafting grid.
  Recoloring preserves custom names and bucket entity data.
- Catch and release dyed fish with water buckets. Colors survive world saves,
  bucket storage, and death drops.
- Find all colored items and buckets in the Kaleidoscope Pufferfish creative tab.

Undyed fish retain their vanilla appearance and behavior. Dyed items retain the
vanilla item IDs and food properties, including pufferfish poisoning. Existing
recipes that accept vanilla pufferfish also accept dyed pufferfish. This does not
add new dishes or change which fish other mods consider edible.

Textures are derived from the active resource pack at load time: warm body pixels
are recolored while neutral pixels and water retain their colors. Resource packs
that replace the yellow body with another hue may need custom models or textures.
Explicit item model overrides from resource packs or other mods take precedence
over the generated colored models.

Entity colors use synchronized NeoForge data attachments. Item colors and names
use vanilla data components, and item models use NeoForge's model baking event.
The remaining mixins handle bucket transfers, color initialization before entity
tracking, entity textures, and migration of older entity saves. Existing colored
items and buckets retain their colors; older items without name components use a
tooltip fallback until recolored.

## Development

Optional integrations are Kaleidoscope Cookery 1.5.1 / 1.6.0 and Farmer's Delight
1.3.4, resolved through Modrinth Maven. Both mods are available at compile time
and loaded in development runs by default; neither is bundled in the output JAR.

The default build uses Cookery 1.5.1. To build or run against 1.6.0:

```powershell
.\gradlew.bat build "-Pkaleidoscope_cookery_version=1.6.0-neoforge+mc1.21.1"
.\gradlew.bat runClient "-Pkaleidoscope_cookery_version=1.6.0-neoforge+mc1.21.1"
```

Use `-Pinclude_cookery=false` or `-Pinclude_farmers_delight=false` to test an
integration separately. Set both to `false` to run without either mod.

```powershell
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient -Pclient_smoke_test=true
```

Game tests cover all colors, initial tracking packets, persistence, legacy save
migration, bucket release, item names, recipe data preservation, dye consumption,
death drops, and Cookery recipe ingredients. The client smoke test checks all
colored models and textures, verifies external model override delegation, reloads
resource packs, and exits automatically. Test sources are excluded from
the published JAR. CI runs game tests against both supported Cookery versions.

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE), except
where otherwise noted. The original NeoForge MDK template files retain their
[MIT license notice](TEMPLATE_LICENSE.txt).

Minecraft mapping names are covered by Mojang's
[mapping license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
