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
            programArguments.addAll(
                "--width", providers.gradleProperty("clientWidth").getOrElse("1280"),
                "--height", providers.gradleProperty("clientHeight").getOrElse("720")
            )
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
    } + listOf("netherite_pufferfish", "golden_pufferfish", "pufferfish_sashimi", "heavy_pufferfish",
        "braised_pufferfish", "braised_pufferfish_rice_bowl", "pufferfish_zosui", "golden_pufferfish_sashimi")
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

val generateSkinResources = tasks.register<Sync>("generateSkinResources") {
    inputs.property("modId", modId)
    inputs.property("colors", dyeColors)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishSkins"))
    for (color in listOf("") + dyeColors) {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val skinName = "${prefix}pufferfish_skin"
        val properties = mapOf(
            "modId" to modId,
            "blockName" to skinName,
            "fishTexture" to (if (color.isEmpty()) "minecraft:item/pufferfish" else "$modId:item/${color}_pufferfish"),
            "sashimiTexture" to "$modId:item/pufferfish_sashimi",
            "blockId" to "$modId:$skinName",
            "ingredient" to JsonOutput.toJson(if (color.isEmpty()) mapOf("type" to "$modId:pufferfish")
                else mapOf("type" to "$modId:pufferfish", "color" to color))
        )
        inputs.property(skinName, properties)
        for ((template, target) in mapOf(
            "skinTemplates/blockstate" to "assets/$modId/blockstates/$skinName.json",
            "skinTemplates/item_model" to "assets/$modId/models/item/$skinName.json",
            "skinTemplates/loot" to "data/$modId/loot_table/blocks/$skinName.json",
            "skinTemplates/block_model" to "assets/$modId/models/block/$skinName.json",
            "skinTemplates/cutting" to "data/$modId/recipe/cutting/$skinName.json",
            "skinTemplates/chopping" to "data/$modId/recipe/chopping/$skinName.json",
            "skinTemplates/pressing" to "data/$modId/recipe/pressing/$skinName.json"
        )) {
            from("src/main/$template.json") {
                expand(properties)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
        for (stage in 0..3) {
            from("src/main/skinTemplates/chopping_stage_$stage.json") {
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
        from("src/main/skinTemplates/$template.json") {
            expand("modId" to modId, "blockName" to "golden_pufferfish_skin",
                "blockId" to "$modId:golden_pufferfish_skin",
                "ingredient" to JsonOutput.toJson(mapOf("item" to "$modId:golden_pufferfish")))
            into(target.substringBeforeLast('/'))
            rename { target.substringAfterLast('/') }
        }
    }
    for (stage in 0..3) {
        from("src/main/skinTemplates/chopping_stage_$stage.json") {
            expand("modId" to modId, "blockName" to "pufferfish_skin",
                "fishTexture" to "$modId:item/golden_pufferfish",
                "sashimiTexture" to "$modId:item/golden_pufferfish_sashimi")
            into("assets/$modId/models/chopping_board/golden_pufferfish")
            rename { "$stage.json" }
        }
    }
}

val generateShieldResources = tasks.register<Sync>("generateShieldResources") {
    inputs.property("modId", modId)
    inputs.property("colors", dyeColors)
    filteringCharset = "UTF-8"
    into(layout.buildDirectory.dir("generated/resources/pufferfishShields"))
    for (color in listOf("") + dyeColors + "golden") {
        val prefix = if (color.isEmpty()) "" else "${color}_"
        val skinName = "${prefix}pufferfish_skin"
        val shieldName = "${skinName}_shield"
        for ((template, target) in mapOf(
            "recipe" to "data/$modId/recipe/$shieldName.json",
            "model" to "assets/$modId/models/item/$shieldName.json",
            "blocking" to "assets/$modId/models/item/${shieldName}_blocking.json",
            "advancement" to "data/$modId/advancement/recipes/combat/$shieldName.json"
        )) {
            from("src/main/shieldTemplates/$template.json") {
                expand("modId" to modId, "skinName" to skinName, "shieldName" to shieldName)
                into(target.substringBeforeLast('/'))
                rename { target.substringAfterLast('/') }
            }
        }
    }
}

sourceSets.main {
    resources.srcDir(generateShieldResources)
    resources.srcDir(generateModMetadata)
    resources.srcDir(generateItemModels)
    resources.srcDir(generateSkinResources)
}
neoForge.ideSyncTask(generateModMetadata)
neoForge.ideSyncTask(generateItemModels)
neoForge.ideSyncTask(generateSkinResources)
neoForge.ideSyncTask(generateShieldResources)

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
