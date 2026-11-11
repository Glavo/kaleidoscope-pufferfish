# JSON field ordering

Run `python tools/order_json.py` to order object fields in resource JSON and Gradle JSON templates. Use `python tools/order_json.py --check` to report drift without changing files. Only the Python standard library is needed; Python is not required to build the mod.

The ordering follows the resource's schema:

- Recipes: loading conditions, type and category, inputs, processing parameters, outputs.
- Models: parent, rendering options, textures, display transforms, elements, overrides. Transforms use rotation, translation, scale; faces use down, up, north, south, west, east.
- Advancements: loading conditions, parent, display, criteria, requirements, rewards.
- Loot: type and pools; pool conditions precede rolls and entries. Entry identity and weight precede conditions and functions.
- Tags: replace, values. Language files and named dictionaries use alphabetical key order.

Unspecified fields follow known fields alphabetically. Arrays, scalar values, escape sequences and template expressions retain their original content and order. Existing compact and multiline layouts are preserved. Duplicate keys are rejected. A structural comparison guards each rewrite against value or array-order changes.

Edit templates rather than their generated copies. JSON objects constructed directly in `build.gradle.kts` should follow the same conventions; generated resources are not rewritten during normal builds.
