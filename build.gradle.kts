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
val copperVariants = listOf("copper", "exposed_copper", "weathered_copper", "oxidized_copper",
    "waxed_copper", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper")
val materialFishVariants = copperVariants + listOf("iron", "golden", "diamond", "netherite", "heavy", "radiant")
val allSkinVariants = listOf("") + dyeColors + materialFishVariants
// Oxidized and waxed copper shields are not registered.
val shieldVariants = allSkinVariants.filter { it !in copperVariants.drop(1) }

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
    exclusiveContent {
        forRepository {
            maven {
                name = "JEI"
                url = uri("https://maven.blamejared.com/")
            }
        }
        filter { includeGroup("mezz.jei") }
    }
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
            programArguments.addAll(
                "--width", providers.gradleProperty("clientWidth").getOrElse("1280"),
                "--height", providers.gradleProperty("clientHeight").getOrElse("720")
            )
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            systemProperty("pufferfish_please.clientSmokeTest", providers.gradleProperty("client_smoke_test").getOrElse("false"))
            systemProperty("pufferfish_please.testImages", providers.gradleProperty("test_images").getOrElse("false"))
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
val localRuntime = configurations.create("localRuntime")
configurations.runtimeClasspath {
    extendsFrom(localRuntime)
}

dependencies {
    val cookery = "maven.modrinth:kaleidoscope-cookery:${providers.gradleProperty("kaleidoscope_cookery_version").get()}"
    val chineseFood = "maven.modrinth:kaleidoscopechinesefood:${providers.gradleProperty("kaleidoscope_chinesefood_version").get()}"
    val farmersDelight = "maven.modrinth:farmers-delight:${providers.gradleProperty("farmers_delight_version").get()}"
    val create = "maven.modrinth:create:${providers.gradleProperty("create_version").get()}"

    compileOnly(cookery)
    compileOnly(chineseFood)
    compileOnly(farmersDelight)
    compileOnly(create)
    compileOnly("mezz.jei:jei-${providers.gradleProperty("minecraft_version").get()}-common-api:${providers.gradleProperty("jei_version").get()}")
    // Create bundles Ponder at runtime; its base interfaces are also needed by javac.
    compileOnly("net.createmod.ponder:ponder-neoforge:${providers.gradleProperty("ponder_version").get()}") {
        isTransitive = false
    }
    if (providers.gradleProperty("include_create").get().toBoolean()) {
        localRuntime(create)
    }
    if (providers.gradleProperty("include_cookery").get().toBoolean()) {
        localRuntime(cookery)
        if (providers.gradleProperty("include_chinesefood").get().toBoolean()) localRuntime(chineseFood)
    }
    if (providers.gradleProperty("include_farmers_delight").get().toBoolean()) {
        localRuntime(farmersDelight)
    }
}

val metadataProperties = listOf(
    "minecraft_version", "minecraft_version_range", "neo_version", "loader_version_range",
    "mod_id", "mod_name", "mod_license", "mod_version",
    "kaleidoscope_cookery_version_range", "farmers_delight_version_range", "create_version_range"
).associateWith { providers.gradleProperty(it).get() }

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.properties(metadataProperties)
    expand(metadataProperties)
    from("src/main/templates/metadata")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

val generateItemModels = tasks.register<Sync>("generateItemModels") {
    val modelNames = dyeColors.flatMap { color ->
        listOf("${color}_pufferfish", "${color}_pufferfish_bucket")
    } + listOf("netherite_pufferfish", "radiant_pufferfish", "golden_pufferfish", "pufferfish_sashimi", "heavy_pufferfish",
        "braised_pufferfish", "braised_pufferfish_rice_bowl", "pufferfish_zosui",
        "raw_pufferfish_baozi", "pufferfish_baozi",
        "copper_pufferfish", "iron_pufferfish", "diamond_pufferfish")
    inputs.property("modId", modId)
    inputs.property("modelNames", modelNames)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/itemModels"))
    for (modelName in modelNames) {
        from("src/main/templates/models/item.json") {
            expand("texture" to "$modId:item/$modelName")
            into("assets/$modId/models/item")
            rename { "$modelName.json" }
        }
    }
}

val generateSkinResources = tasks.register<Sync>("generateSkinResources") {
    inputs.property("modId", modId)
    inputs.property("variants", allSkinVariants)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishSkins"))
    for (color in allSkinVariants.filter { it != "golden" }) {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val skinName = "${prefix}pufferfish_skin"
        val properties = mapOf(
            "modId" to modId,
            "blockName" to skinName,
            "fishTexture" to (if (color.isEmpty()) "minecraft:item/pufferfish" else "$modId:item/${color.removePrefix("waxed_")}_pufferfish"),
            "sashimiTexture" to "$modId:item/pufferfish_sashimi",
            "blockId" to "$modId:$skinName",
            "ingredient" to JsonOutput.toJson(when (color) {
                "" -> mapOf("type" to "$modId:pufferfish")
                "netherite", "radiant", "heavy", "copper", "iron", "diamond", "exposed_copper", "weathered_copper", "oxidized_copper",
                "waxed_copper", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper" -> mapOf("item" to "$modId:${color}_pufferfish")
                else -> mapOf("type" to "$modId:pufferfish", "color" to color)
            })
        )
        inputs.property(skinName, properties)
        for ((template, target) in mapOf(
            "skins/blockstate" to "assets/$modId/blockstates/$skinName.json",
            "skins/item_model" to "assets/$modId/models/item/$skinName.json",
            "skins/loot" to "data/$modId/loot_table/blocks/$skinName.json",
            "skins/block_model" to "assets/$modId/models/block/$skinName.json",
            "skins/cutting" to "data/$modId/recipe/cutting/$skinName.json",
            "skins/chopping" to "data/$modId/recipe/chopping/$skinName.json",
            "skins/pressing" to "data/$modId/recipe/pressing/$skinName.json"
        )) {
            from("src/main/templates/$template.json") {
                expand(properties)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
        for (stage in 0..3) {
            from("src/main/templates/skins/chopping_stage_$stage.json") {
                expand(properties)
                into("assets/$modId/models/chopping_board/$skinName")
                rename { "$stage.json" }
            }
        }
    }
    for ((template, target) in mapOf(
        "blockstate" to "assets/$modId/blockstates/golden_pufferfish_skin.json",
        "block_model" to "assets/$modId/models/block/golden_pufferfish_skin.json",
        "item_model" to "assets/$modId/models/item/golden_pufferfish_skin.json",
        "loot" to "data/$modId/loot_table/blocks/golden_pufferfish_skin.json",
        "pressing" to "data/$modId/recipe/pressing/golden_pufferfish_skin.json"
    )) {
        from("src/main/templates/skins/$template.json") {
            expand("modId" to modId, "blockName" to "golden_pufferfish_skin",
                "blockId" to "$modId:golden_pufferfish_skin",
                "ingredient" to JsonOutput.toJson(mapOf("item" to "$modId:golden_pufferfish")))
            into(target.substringBeforeLast('/'))
            rename { target.substringAfterLast('/') }
        }
    }
    val goldenChoppingProperties = mapOf(
        "modId" to modId, "blockName" to "golden_pufferfish_skin",
        "fishTexture" to "$modId:item/golden_pufferfish",
        "sashimiTexture" to "$modId:item/pufferfish_sashimi"
    )
    inputs.property("goldenChopping", goldenChoppingProperties)
    for (stage in 0..3) {
        from("src/main/templates/skins/chopping_stage_$stage.json") {
            expand(goldenChoppingProperties)
            into("assets/$modId/models/chopping_board/golden_pufferfish")
            rename { "$stage.json" }
        }
    }
}

val generateMobResources = tasks.register("generateMobResources") {
    // Capture values instead of the build script in the configuration-cached task action.
    val namespace = modId
    val variants = materialFishVariants.toList()
    inputs.property("modId", namespace)
    inputs.property("variants", variants)
    val destination = layout.buildDirectory.dir("generated/resources/mobs")
    outputs.dir(destination)
    doLast {
        fun write(path: String, value: Any) {
            val file = destination.get().file(path).asFile
            file.parentFile.mkdirs()
            file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(value)) + "\n")
        }
        for (variant in variants) {
            val name = "${variant}_pufferfish"
            write("assets/$namespace/models/item/${name}_bucket.json", mapOf("parent" to "minecraft:item/generated",
                "textures" to mapOf("layer0" to "$namespace:item/${name}_bucket")))
            write("assets/$namespace/models/item/${name}_spawn_egg.json", mapOf("parent" to "minecraft:item/template_spawn_egg"))
            write("data/$namespace/loot_table/entities/$name.json", mapOf("type" to "minecraft:entity",
                "pools" to listOf(mapOf("rolls" to 1, "entries" to listOf(mapOf("type" to "minecraft:item", "name" to "$namespace:$name"))))))
        }
        write("data/$namespace/neoforge/biome_modifier/material_pufferfish.json", mapOf("type" to "neoforge:add_spawns",
            "biomes" to listOf("minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean"),
            "spawners" to variants.filter { !it.startsWith("waxed_") }.map {
                mapOf("type" to "$namespace:${it}_pufferfish", "weight" to 1, "minCount" to 1, "maxCount" to 2)
            }))
        for (tag in listOf("not_scary_for_pufferfish", "sensitive_to_impaling", "aquatic")) {
            write("data/minecraft/tags/entity_type/$tag.json", mapOf("replace" to false,
                "values" to variants.map { "$namespace:${it}_pufferfish" }))
        }
    }
}

val generateShieldResources = tasks.register<Sync>("generateShieldResources") {
    inputs.property("modId", modId)
    inputs.property("variants", shieldVariants)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishShields"))
    for (color in shieldVariants) {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val skinName = "${prefix}pufferfish_skin"
        val shieldName = "${skinName}_shield"
        for ((template, target) in mapOf(
            "recipe" to "data/$modId/recipe/$shieldName.json",
            "model" to "assets/$modId/models/item/$shieldName.json",
            "blocking" to "assets/$modId/models/item/${shieldName}_blocking.json",
            "advancement" to "data/$modId/advancement/recipes/combat/$shieldName.json"
        )) {
            from("src/main/templates/shields/$template.json") {
                expand("modId" to modId, "skinName" to skinName, "shieldName" to shieldName)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
    }
}

val generateArmorResources = tasks.register<Sync>("generateArmorResources") {
    val patterns = mapOf(
        "helmet" to listOf("SSS", "S S"),
        "chestplate" to listOf("S S", "SSS", "SSS"),
        "leggings" to listOf("SSS", "S S", "S S"),
        "boots" to listOf("S S", "S S")
    )
    inputs.property("modId", modId)
    inputs.property("variants", allSkinVariants)
    inputs.property("patterns", patterns)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishArmor"))
    for (color in allSkinVariants) {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val skinName = "${prefix}pufferfish_skin"
        val catArmorName = "${skinName}_cat_armor"
        for ((template, target) in mapOf(
            "recipe" to "data/$modId/recipe/$catArmorName.json",
            "advancement" to "data/$modId/advancement/recipes/combat/$catArmorName.json"
        )) {
            from("src/main/templates/armor/$template.json") {
                expand("modId" to modId, "skinName" to skinName, "armorName" to catArmorName,
                    "armorType" to "cat_armor", "pattern" to JsonOutput.toJson(listOf("S S", "SSS", " S ")))
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
        from("src/main/templates/armor/cat_model.json") {
            expand("modId" to modId, "skinName" to skinName)
            into("assets/$modId/models/item")
            rename { "$catArmorName.json" }
        }
        for ((armorType, pattern) in patterns) {
            val armorName = "${skinName}_$armorType"
            for ((template, target) in mapOf(
                "recipe" to "data/$modId/recipe/$armorName.json",
                "model" to "assets/$modId/models/item/$armorName.json",
                "advancement" to "data/$modId/advancement/recipes/combat/$armorName.json"
            )) {
                from("src/main/templates/armor/$template.json") {
                    expand("modId" to modId, "skinName" to skinName, "armorName" to armorName,
                        "armorType" to armorType, "pattern" to JsonOutput.toJson(pattern))
                    into(target.substringBeforeLast('/'))
                    rename { target.substringAfterLast('/') }
                }
            }
        }
    }
    for ((armorType, slot) in mapOf("helmet" to "head", "chestplate" to "chest", "leggings" to "leg", "boots" to "foot")) {
        from("src/main/templates/armor/tag.json") {
            expand("values" to JsonOutput.toJson((allSkinVariants).map { color ->
                val prefix = if (color.isEmpty()) "" else "${color}_"
                "$modId:${prefix}pufferfish_skin_$armorType"
            }))
            into("data/minecraft/tags/item")
            rename { "${slot}_armor.json" }
        }
    }
}

val generateBoatResources = tasks.register<Sync>("generateBoatResources") {
    inputs.property("modId", modId)
    inputs.property("variants", allSkinVariants)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishBoats"))
    for (color in allSkinVariants) {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val skinName = "${prefix}pufferfish_skin"
        val boatName = "${skinName}_boat"
        for ((template, target) in mapOf(
            "recipe" to "data/$modId/recipe/$boatName.json",
            "model" to "assets/$modId/models/item/$boatName.json",
            "advancement" to "data/$modId/advancement/recipes/transportation/$boatName.json"
        )) {
            from("src/main/templates/boats/$template.json") {
                expand("modId" to modId, "skinName" to skinName, "boatName" to boatName)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
    }
}

val generatePieModels = tasks.register<Sync>("generatePieModels") {
    inputs.property("modId", modId)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishPie"))
    fun uv(tile: Int) = listOf(tile % 4 * 4, tile / 4 * 4, tile % 4 * 4 + 4, tile / 4 * 4 + 4)
    fun box(x: Double, y: Double, z: Double, width: Double, height: Double, depth: Double, tile: Int, top: Int = tile): Map<String, Any> = mapOf(
        "from" to listOf(x, y, z), "to" to listOf(x + width, y + height, z + depth),
        "faces" to listOf("down", "up", "north", "south", "west", "east").associateWith {
            mapOf("uv" to uv(if (it == "up") top else tile), "texture" to "#pie")
        }
    )
    fun fish(x: Double, z: Double): List<Map<String, Any>> {
        val body = box(x - 1.8, 3.8, z - 1.8, 3.6, 3.6, 3.6, 1, 0).toMutableMap()
        body["faces"] = mapOf("down" to 1, "up" to 0, "north" to 1, "south" to 6, "west" to 8, "east" to 7)
            .mapValues { (_, tile) -> mapOf("uv" to uv(tile), "texture" to "#pie") }
        val fins = listOf(box(x - 2.8, 6.2, z - 0.6, 1.0, 0.3, 1.2, 5),
            box(x + 1.8, 6.2, z - 0.6, 1.0, 0.3, 1.2, 5))
        val spines = listOf(-1.0, 1.0).flatMap { side ->
            listOf(-1.2, 1.2).flatMap { offset -> listOf(
                box(x + side * 1.6 - 0.2, 7.4, z + offset - 0.2, 0.4, 0.8, 0.4, 9),
                box(x + side * 2.15 - 0.35, 5.0, z + offset - 0.2, 0.7, 0.4, 0.4, 9),
                box(x + offset - 0.2, 5.6, z + side * 2.15 - 0.35, 0.4, 0.4, 0.7, 9)
            ) }
        }
        return listOf(body) + fins + spines
    }
    val plate = listOf(box(2.0, 0.0, 1.0, 12.0, 1.0, 14.0, 4), box(1.0, 0.0, 2.0, 1.0, 1.0, 12.0, 4),
        box(14.0, 0.0, 2.0, 1.0, 1.0, 12.0, 4))
    val portions = listOf(2.0 to 2.0, 8.0 to 2.0, 2.0 to 8.0, 8.0 to 8.0).mapIndexed { index, (x, z) ->
        listOf(box(x, 1.0, z, 6.0, 2.0, 6.0, 2), box(x + 0.3, 3.0, z + 0.3, 5.4, 1.0, 5.4, 3)) +
                fish(if (x == 2.0) 4.0 else 12.0, if (z == 2.0) 4.0 else 12.0) +
                if (index == 3) fish(8.0, 8.0) else emptyList()
    }
    for (bites in 0..4) {
        val elements = JsonOutput.toJson(plate + portions.drop(bites).flatten())
        inputs.property("elements$bites", elements)
        from("src/main/templates/models/pufferfish_pie.json") {
            expand("modId" to modId, "elements" to elements)
            into("assets/$modId/models/block")
            rename { "pufferfish_stargazy_pie_$bites.json" }
        }
    }
}

val generateAspicModels = tasks.register<Sync>("generateAspicModels") {
    inputs.property("modId", modId)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishAspic"))
    fun box(x: Double, y: Double, z: Double, w: Double, h: Double, d: Double, tile: Int) = mapOf(
        "from" to listOf(x, y, z), "to" to listOf(x + w, y + h, z + d),
        "faces" to listOf("down", "up", "north", "south", "west", "east").associateWith {
            mapOf("uv" to listOf(tile % 4 * 4, tile / 4 * 4, tile % 4 * 4 + 4, tile / 4 * 4 + 4), "texture" to "#food")
        }
    )
    val bowl = listOf(
        box(3.0, 0.0, 3.0, 10.0, 1.0, 10.0, 1), box(2.0, 1.0, 2.0, 12.0, 1.0, 12.0, 0),
        box(2.0, 2.0, 2.0, 12.0, 2.0, 1.0, 0), box(2.0, 2.0, 13.0, 12.0, 2.0, 1.0, 0),
        box(2.0, 2.0, 3.0, 1.0, 2.0, 10.0, 0), box(13.0, 2.0, 3.0, 1.0, 2.0, 10.0, 0)
    )
    val portions = listOf(3.5 to 3.5, 8.5 to 3.5, 3.5 to 8.5, 8.5 to 8.5).map { (x, z) -> listOf(
        box(x + 0.7, 3.0, z + 0.8, 2.3, 0.6, 1.2, 5), box(x + 1.1, 4.3, z + 2.1, 1.7, 0.4, 1.0, 6),
        box(x, 2.0, z, 4.0, 4.0, 4.0, 3), box(x + 0.3, 6.0, z + 0.4, 1.2, 0.15, 0.5, 4)
    ) }
    for (bites in 0..4) {
        val elements = JsonOutput.toJson(bowl + portions.drop(bites).flatten())
        inputs.property("elements$bites", elements)
        from("src/main/templates/models/pufferfish_aspic.json") {
            expand("modId" to modId, "elements" to elements)
            into("assets/$modId/models/block")
            rename { "pufferfish_aspic_$bites.json" }
        }
    }
    val broth = JsonOutput.toJson(bowl + listOf(box(3.0, 2.0, 3.0, 10.0, 1.4, 10.0, 2),
        box(4.0, 3.4, 5.0, 3.0, 0.1, 1.5, 5), box(8.0, 3.4, 8.0, 3.0, 0.1, 2.0, 6)))
    inputs.property("broth", broth)
    from("src/main/templates/models/pufferfish_aspic.json") {
        expand("modId" to modId, "elements" to broth)
        into("assets/$modId/models/block")
        rename { "pufferfish_skin_broth.json" }
    }
}

val generateLampResources = tasks.register<Sync>("generateLampResources") {
    inputs.property("modId", modId)
    inputs.property("variants", allSkinVariants)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishLamps"))
    for (variant in allSkinVariants.filter { it != "radiant" }) {
        val fishName = (if (variant.isEmpty()) "" else "${variant}_") + "pufferfish"
        val lampName = "${fishName}_lamp"
        val ingredient = when {
            variant.isEmpty() -> mapOf("type" to "$modId:pufferfish")
            variant in dyeColors -> mapOf("type" to "$modId:pufferfish", "color" to variant)
            else -> mapOf("item" to "$modId:$fishName")
        }
        for ((template, target) in mapOf(
            "recipe" to "data/$modId/recipe/$lampName.json",
            "advancement" to "data/$modId/advancement/recipes/building_blocks/$lampName.json",
            "blockstate" to "assets/$modId/blockstates/$lampName.json",
            "block_model" to "assets/$modId/models/block/$lampName.json",
            "item_model" to "assets/$modId/models/item/$lampName.json"
        )) {
            from("src/main/templates/lamps/$template.json") {
                expand("modId" to modId, "lampName" to lampName, "ingredient" to JsonOutput.toJson(ingredient),
                    "fishTexture" to (if (variant.isEmpty()) "minecraft:item/pufferfish" else "$modId:item/${fishName.removePrefix("waxed_")}"))
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
    }
}
val generateFlailResources = tasks.register<Sync>("generateFlailResources") {
    val materials = materialFishVariants.filter { it != "netherite" && it != "heavy" }
    inputs.property("modId", modId)
    inputs.property("materials", materials)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishFlails"))
    for (material in materials) {
        val fishName = "${material}_pufferfish"
        val flailName = "${fishName}_flail"
        for ((template, target) in mapOf(
            "recipe" to "data/$modId/recipe/$flailName.json",
            "advancement" to "data/$modId/advancement/recipes/combat/$flailName.json",
            "model" to "assets/$modId/models/item/$flailName.json"
        )) {
            from("src/main/templates/flails/$template.json") {
                expand("modId" to modId, "fishName" to fishName, "flailName" to flailName,
                    "textureName" to fishName.removePrefix("waxed_"))
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
    }
}
val generateMaterialFishRecipes = tasks.register<Sync>("generateMaterialFishRecipes") {
    val materials = mapOf("copper" to "copper_ingot", "iron" to "iron_ingot", "diamond" to "diamond")
    inputs.property("modId", modId)
    inputs.property("materials", materials)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/materialFish"))
    for ((name, ingredient) in materials) {
        from("src/main/templates/material_fish/recipe.json") {
            expand("modId" to modId, "name" to name, "ingredient" to ingredient)
            into("data/$modId/recipe")
            rename { "${name}_pufferfish.json" }
        }
        from("src/main/templates/material_fish/advancement.json") {
            expand("modId" to modId, "name" to name)
            into("data/$modId/advancement/recipes/combat")
            rename { "${name}_pufferfish.json" }
        }
    }
}

sourceSets.main {
    resources.srcDir(generateMobResources)
    resources.srcDir(generateLampResources)
    resources.srcDir(generateFlailResources)
    resources.srcDir(generateMaterialFishRecipes)
    resources.srcDir(generatePieModels)
    resources.srcDir(generateAspicModels)
    resources.srcDir(generateBoatResources)
    resources.srcDir(generateArmorResources)
    resources.srcDir(generateShieldResources)
    resources.srcDir(generateModMetadata)
    resources.srcDir(generateItemModels)
    resources.srcDir(generateSkinResources)
}
neoForge.ideSyncTask(generateLampResources)
neoForge.ideSyncTask(generateMobResources)
neoForge.ideSyncTask(generateFlailResources)
neoForge.ideSyncTask(generateModMetadata)
neoForge.ideSyncTask(generateItemModels)
neoForge.ideSyncTask(generateSkinResources)
neoForge.ideSyncTask(generateShieldResources)
neoForge.ideSyncTask(generateArmorResources)
neoForge.ideSyncTask(generateBoatResources)

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
