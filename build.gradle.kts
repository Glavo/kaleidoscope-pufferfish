plugins {
    id("java-library")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.148"
    id("idea")
    id("org.glavo.gradle-wrapper-neo") version "0.2.0"
}

val modId = providers.gradleProperty("mod_id").get()

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

repositories {
    mavenCentral()
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
        }
        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("gameTestServer") {
            type.set("gameTestServer")
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

    compileOnly(cookery)
    compileOnly(farmersDelight)
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
    "kaleidoscope_cookery_version_range", "farmers_delight_version_range"
).associateWith { providers.gradleProperty(it).get() }

val generateModMetadata by tasks.registering(ProcessResources::class) {
    inputs.properties(metadataProperties)
    expand(metadataProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main {
    resources.srcDir(generateModMetadata)
}
neoForge.ideSyncTask(generateModMetadata)

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

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
