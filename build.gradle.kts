import groovy.json.JsonOutput

plugins {
    id("java-library")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.148"
    id("idea")
    id("org.glavo.gradle-wrapper-neo") version "0.2.0"
}

val modId = providers.gradleProperty("mod_id").get()
val dyeColors = listOf(
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
)

group = providers.gradleProperty("mod_group_id").get()
version = providers.gradleProperty("mod_version").get()

base {
    archivesName.set(modId)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.BIN
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel", "src/generated/**/.cache")
    }
}

val gameTest = sourceSets.create("gameTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

repositories {
    mavenCentral()
    maven {
        name = "Create"
        url = uri("https://maven.createmod.net")
        content { includeGroup("net.createmod.ponder") }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

neoForge {
    version = providers.gradleProperty("neo_version").get()

    parchment {
        mappingsVersion.set(providers.gradleProperty("parchment_mappings_version"))
        minecraftVersion.set(providers.gradleProperty("parchment_minecraft_version"))
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            systemProperty("kaleidoscope_pufferfish.clientSmokeTest", providers.gradleProperty("client_smoke_test").getOrElse("false"))
        }
        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("gameTestServer") {
            type.set("gameTestServer")
            sourceSet.set(gameTest)
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("data") {
            data()
            programArguments.addAll(
                "--mod", modId, "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel.set(org.slf4j.event.Level.DEBUG)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(gameTest)
        }
    }
}

// Development-only dependencies are excluded from published dependency metadata.
val localRuntime by configurations.creating
configurations.runtimeClasspath {
    extendsFrom(localRuntime)
}

dependencies {
    val cookery = "maven.modrinth:kaleidoscope-cookery:${providers.gradleProperty("kaleidoscope_cookery_version").get()}"
    val farmersDelight = "maven.modrinth:farmers-delight:${providers.gradleProperty("farmers_delight_version").get()}"
    val create = "maven.modrinth:create:${providers.gradleProperty("create_version").get()}"

    compileOnly(cookery)
    compileOnly(farmersDelight)
    compileOnly(create)
    // Create bundles Ponder at runtime; its base interfaces are also needed by javac.
    compileOnly("net.createmod.ponder:ponder-neoforge:${providers.gradleProperty("ponder_version").get()}") {
        isTransitive = false
    }
    if (providers.gradleProperty("include_create").get().toBoolean()) {
        localRuntime(create)
    }
    if (providers.gradleProperty("include_cookery").get().toBoolean()) {
        localRuntime(cookery)
    }
    if (providers.gradleProperty("include_farmers_delight").get().toBoolean()) {
        localRuntime(farmersDelight)
    }

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val metadataProperties = listOf(
    "minecraft_version", "minecraft_version_range", "neo_version", "loader_version_range",
    "mod_id", "mod_name", "mod_license", "mod_version",
    "kaleidoscope_cookery_version_range", "farmers_delight_version_range", "create_version_range"
).associateWith { providers.gradleProperty(it).get() }

val generateModMetadata by tasks.registering(ProcessResources::class) {
    inputs.properties(metadataProperties)
    expand(metadataProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

val generateItemModels = tasks.register<Sync>("generateItemModels") {
    val modelNames = dyeColors.flatMap { color ->
        listOf("${color}_pufferfish", "${color}_pufferfish_bucket")
    } + listOf("netherite_pufferfish", "golden_pufferfish")
    inputs.property("modId", modId)
    inputs.property("modelNames", modelNames)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/itemModels"))
    for (modelName in modelNames) {
        from("src/main/modelTemplates/item.json") {
            expand("texture" to "$modId:item/$modelName")
            into("assets/$modId/models/item")
            rename { "$modelName.json" }
        }
    }
}

val generateBlockResources = tasks.register<Sync>("generateBlockResources") {
    inputs.property("modId", modId)
    inputs.property("colors", dyeColors)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishBlocks"))
    for (color in listOf("") + dyeColors + "netherite") {
        val netherite = color == "netherite"
        val blockName = if (color.isEmpty()) "pufferfish_block" else "${color}_pufferfish_block"
        val blockId = "$modId:$blockName"
        val fishId = if (netherite) "$modId:netherite_pufferfish" else "minecraft:pufferfish"
        val components = if (color.isEmpty() || netherite) "{}" else JsonOutput.toJson(mapOf(
            "$modId:color" to color,
            "minecraft:item_name" to JsonOutput.toJson(mapOf(
                "translate" to "item.$modId.dyed_pufferfish",
                "with" to listOf(mapOf("translate" to "color.minecraft.$color"))
            ))
        ))
        val properties = mapOf(
            "modId" to modId,
            "blockName" to blockName,
            "blockId" to blockId,
            "fishId" to fishId,
            "ingredient" to JsonOutput.toJson(when {
                netherite -> mapOf("item" to fishId)
                color.isEmpty() -> mapOf("type" to "$modId:pufferfish")
                else -> mapOf("type" to "$modId:pufferfish", "color" to color)
            }),
            "components" to components
        )
        inputs.property(blockName, properties)
        for ((template, target) in mapOf(
            "blockstate" to "assets/$modId/blockstates/$blockName.json",
            "block_model" to "assets/$modId/models/block/$blockName.json",
            "item_model" to "assets/$modId/models/item/$blockName.json",
            "loot" to "data/$modId/loot_table/blocks/$blockName.json",
            "compress" to "data/$modId/recipe/$blockName.json",
            "decompress" to "data/$modId/recipe/${blockName}_unpacking.json"
        )) {
            from("src/main/blockTemplates/$template.json") {
                expand(properties)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
        for ((recipe, ingredient) in mapOf(blockName to fishId, "${blockName}_unpacking" to blockId)) {
            from("src/main/blockTemplates/advancement.json") {
                expand("recipe" to "$modId:$recipe", "ingredient" to ingredient)
                into("data/$modId/advancement/recipes/misc")
                rename { "$recipe.json" }
            }
        }
    }
}

val generateCarpetResources = tasks.register<Sync>("generateCarpetResources") {
    inputs.property("modId", modId)
    inputs.property("colors", dyeColors)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishCarpets"))
    for (color in listOf("") + dyeColors) {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val carpetName = "${prefix}pufferfish_carpet"
        val properties = mapOf(
            "modId" to modId,
            "blockName" to carpetName,
            "blockId" to "$modId:$carpetName",
            "ingredient" to JsonOutput.toJson(if (color.isEmpty()) mapOf("type" to "$modId:pufferfish")
                else mapOf("type" to "$modId:pufferfish", "color" to color))
        )
        inputs.property(carpetName, properties)
        for ((template, target) in mapOf(
            "blockTemplates/blockstate" to "assets/$modId/blockstates/$carpetName.json",
            "blockTemplates/item_model" to "assets/$modId/models/item/$carpetName.json",
            "blockTemplates/loot" to "data/$modId/loot_table/blocks/$carpetName.json",
            "carpetTemplates/block_model" to "assets/$modId/models/block/$carpetName.json",
            "carpetTemplates/crafting" to "data/$modId/recipe/$carpetName.json",
            "carpetTemplates/pressing" to "data/$modId/recipe/pressing/$carpetName.json"
        )) {
            from("src/main/$template.json") {
                expand(properties)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
        from("src/main/blockTemplates/advancement.json") {
            expand("recipe" to "$modId:$carpetName", "ingredient" to "minecraft:pufferfish")
            into("data/$modId/advancement/recipes/decorations")
            rename { "$carpetName.json" }
        }
    }
}

sourceSets.main {
    resources.srcDir(generateModMetadata)
    resources.srcDir(generateItemModels)
    resources.srcDir(generateBlockResources)
    resources.srcDir(generateCarpetResources)
}
neoForge.ideSyncTask(generateModMetadata)
neoForge.ideSyncTask(generateItemModels)
neoForge.ideSyncTask(generateBlockResources)
neoForge.ideSyncTask(generateCarpetResources)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(layout.projectDirectory.dir("repo"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(files("LICENSE", "TEMPLATE_LICENSE.txt")) {
        into("META-INF")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
