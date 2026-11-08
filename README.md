
# Development dependencies

Targets Minecraft 1.21.1 and NeoForge with Java 21. Optional integrations are
Kaleidoscope Cookery 1.5.1 / 1.6.0 and Farmer's Delight 1.3.4, resolved through
Modrinth Maven. Both mods are available at compile time and loaded in development
runs by default; neither is bundled in the output JAR.

The default build uses Cookery 1.5.1. To build or run against 1.6.0:

```powershell
.\gradlew.bat build "-Pkaleidoscope_cookery_version=1.6.0-neoforge+mc1.21.1"
.\gradlew.bat runClient "-Pkaleidoscope_cookery_version=1.6.0-neoforge+mc1.21.1"
```

Use `-Pinclude_cookery=false` or `-Pinclude_farmers_delight=false` with
`runClient` / `runServer` to test an integration separately. Set both to `false`
to run without either mod. CI builds against both supported Cookery versions.

Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
