// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.glavo.mcmod.pufferfish.compat.create.CreateIntegration;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;

@Mod(PufferfishPlease.MODID)
public class PufferfishPlease {
    public static final String MODID = "pufferfish_please";
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    private static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> COLOR = COMPONENTS.register("color",
            () -> DataComponentType.<DyeColor>builder().persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DyeColor>> ENTITY_COLOR = ATTACHMENTS.register("color",
            () -> AttachmentType.builder(() -> DyeColor.WHITE).serialize(DyeColor.CODEC).sync(DyeColor.STREAM_CODEC).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> FISH_ITEM = ATTACHMENTS.register("fish_item",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).build());
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownPufferfish>> THROWN_PUFFERFISH = ENTITIES.register("thrown_pufferfish",
            () -> EntityType.Builder.<ThrownPufferfish>of(ThrownPufferfish::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(id("thrown_pufferfish").toString()));
    /// The transient tethered head; the weapon remains in the player's inventory.
    public static final DeferredHolder<EntityType<?>, EntityType<PufferfishFlailHead>> FLAIL_HEAD = ENTITIES.register("pufferfish_flail_head",
            () -> EntityType.Builder.<PufferfishFlailHead>of(PufferfishFlailHead::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).noSave()
                    .build(id("pufferfish_flail_head").toString()));
    public static final DeferredHolder<RecipeSerializer<?>, PufferfishShieldRecipe.Serializer> SHIELD_RECIPE =
            RECIPES.register("pufferfish_skin_shield", PufferfishShieldRecipe.Serializer::new);
    /// The shaped flail recipe serializer, which retains the ingredient fish's component changes.
    public static final DeferredHolder<RecipeSerializer<?>, PufferfishFlailRecipe.Serializer> FLAIL_RECIPE =
            RECIPES.register("pufferfish_flail", PufferfishFlailRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, PufferfishMaterialRecipe.Serializer> MATERIAL_RECIPE =
            RECIPES.register("material_pufferfish", PufferfishMaterialRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PufferfishDyeRecipe>> DYE_RECIPE =
            RECIPES.register("dye_pufferfish", () -> new SimpleCraftingRecipeSerializer<>(PufferfishDyeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PufferfishBucketRecipe>> BUCKET_RECIPE =
            RECIPES.register("bucket_pufferfish", () -> new SimpleCraftingRecipeSerializer<>(PufferfishBucketRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CopperPufferfishWaxRecipe>> WAX_RECIPE =
            RECIPES.register("wax_copper_pufferfish", () -> new SimpleCraftingRecipeSerializer<>(CopperPufferfishWaxRecipe::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<PufferfishIngredient>> PUFFERFISH_INGREDIENT =
            INGREDIENTS.register("pufferfish", () -> new IngredientType<>(PufferfishIngredient.CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, NetheritePufferfishUpgradeRecipe.Serializer> NETHERITE_UPGRADE =
            RECIPES.register("netherite_pufferfish_upgrade", NetheritePufferfishUpgradeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RadiantPufferfishUpgradeRecipe.Serializer> RADIANT_UPGRADE =
            RECIPES.register("radiant_pufferfish_upgrade", RadiantPufferfishUpgradeRecipe.Serializer::new);

    /// The smithing upgrade from netherite to heavy pufferfish.
    public static final DeferredHolder<RecipeSerializer<?>, HeavyPufferfishUpgradeRecipe.Serializer> HEAVY_UPGRADE =
            RECIPES.register("heavy_pufferfish_upgrade", HeavyPufferfishUpgradeRecipe.Serializer::new);

    static {
        TABS.register("pufferfish", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.pufferfish_please"))
                .icon(() -> PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), DyeColor.PURPLE))
                .displayItems((parameters, output) -> {
                    output.accept(PufferfishBlocks.CONDUIT);
                    output.accept(PufferfishBlocks.ANCIENT_CONDUIT);
                    output.accept(PufferfishBlocks.ANCIENT_PRISMARINE);
                    output.accept(PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE);
                    PufferfishLamps.VARIANTS.values().forEach(variant -> output.accept(variant.block().get()));
                    if (ModList.get().isLoaded("create")) {
                        output.accept(CreateIntegration.ENGINE_ITEM.get());
                    }
                    PufferfishVariant.ALL.stream().filter(variant -> variant != PufferfishVariant.NORMAL)
                            .forEach(variant -> output.accept(variant.fish()));
                    for (var color : DyeColor.values()) {
                        output.accept(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), color));
                    }
                    PufferfishMobs.VARIANTS.values().forEach(variant -> output.accept(variant.bucket()));
                    output.accept(PufferfishItems.SASHIMI);
                    output.accept(PufferfishItems.BRAISED);
                    output.accept(PufferfishItems.BRAISED_RICE_BOWL);
                    output.accept(PufferfishItems.ZOSUI);
                    output.accept(PufferfishItems.RAW_BAOZI);
                    output.accept(PufferfishItems.BAOZI);
                    if (ModList.get().isLoaded("kaleidoscope_cookery")) {
                        output.accept(org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration.STARGAZY_PIE_ITEM);
                        if (ModList.get().isLoaded("kaleidoscope_chinesefood")) {
                            output.accept(PufferfishItems.SKIN_BROTH);
                            output.accept(org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration.ASPIC_ITEM);
                        }
                    }
                    output.accept(PufferfishItems.FLAIL);
                    for (var color : DyeColor.values()) {
                        var flail = new ItemStack(PufferfishItems.FLAIL.get());
                        flail.set(COLOR.get(), color);
                        output.accept(flail);
                    }
                    PufferfishItems.FLAIL_VARIANTS.stream().skip(1).forEach(output::accept);
                    output.accept(PufferfishItems.PUMP);
                    output.accept(PufferfishItems.UPGRADE_TEMPLATE);
                    PufferfishVariant.ALL.forEach(variant -> output.accept(variant.skin()));
                    PufferfishShields.VARIANTS.values().forEach(output::accept);
                    PufferfishArmor.VARIANTS.values().forEach(output::accept);
                    PufferfishBoats.VARIANTS.values().forEach(variant -> output.accept(variant.item()));
                }).build());
    }

    public PufferfishPlease(IEventBus bus, ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, PufferfishMobConfig.SPEC,
                MODID + "-mobs.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, PufferfishCombatConfig.SPEC,
                MODID + "-combat.toml");
        if (ModList.get().isLoaded("create")) {
            CreateIntegration.register(bus, container);
        }
        PufferfishItems.register(bus);
        PufferfishEffects.register(bus);
        PufferfishMobs.register(bus);
        if (ModList.get().isLoaded("kaleidoscope_chinesefood")) {
            org.glavo.mcmod.pufferfish.compat.chinesefood.ChineseFoodIntegration.register(bus);
        }
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration.register(bus);
        }
        PufferfishShields.register(bus);
        PufferfishArmor.register(bus);
        PufferfishBoats.register(bus);
        PufferfishLamps.register(bus);
        PufferfishBlocks.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        ENTITIES.register(bus);
        RECIPES.register(bus);
        INGREDIENTS.register(bus);
        TABS.register(bus);
        bus.addListener(FlailAttackPayload::register);
        NeoForge.EVENT_BUS.addListener(CopperPufferfish::interact);
        NeoForge.EVENT_BUS.addListener(PufferfishColors::interact);
        NeoForge.EVENT_BUS.addListener(PufferfishColors::colorDrops);
        NeoForge.EVENT_BUS.addListener(PufferfishMobs::naturalColors);
        NeoForge.EVENT_BUS.addListener(PufferfishZombieHeadgear::prepare);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, PufferfishZombieHeadgear::equip);
        NeoForge.EVENT_BUS.addListener(PufferfishFriendlyFire::damage);
        NeoForge.EVENT_BUS.addListener(PufferfishFriendlyFire::effect);
        NeoForge.EVENT_BUS.addListener(PufferfishMobThorns::retaliate);
        NeoForge.EVENT_BUS.addListener(PufferfishColors::legacyTooltip);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishThrowing::use);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishPlacement::use);
        NeoForge.EVENT_BUS.addListener(PufferfishCombat::attributes);
        NeoForge.EVENT_BUS.addListener(PufferfishCombat::meleeHit);
        NeoForge.EVENT_BUS.addListener(PufferfishDurability::beforeAttack);
        NeoForge.EVENT_BUS.addListener(PufferfishDurability::beforeDamage);
        NeoForge.EVENT_BUS.addListener(PufferfishDurability::repair);
        NeoForge.EVENT_BUS.addListener(HeavyPufferfishBlock::poisonOnImpact);
        NeoForge.EVENT_BUS.addListener(PufferfishContactDamage::tick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishConduitSpawning::potentialSpawns);
        NeoForge.EVENT_BUS.addListener(PufferfishConduitSpawning::unload);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishEnchantments::applyBook);
        NeoForge.EVENT_BUS.addListener(PufferfishHeadgear::breathe);
        NeoForge.EVENT_BUS.addListener(PufferfishHeadgear::breatheZombie);
        NeoForge.EVENT_BUS.addListener(PufferfishHeadgear::retaliate);
        NeoForge.EVENT_BUS.addListener(PufferfishArmor::retaliate);
        NeoForge.EVENT_BUS.addListener(PufferfishRetaliation::effectsOnThorns);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishShields::retaliate);
        NeoForge.EVENT_BUS.addListener(PufferfishDurability::afterDamage);
        NeoForge.EVENT_BUS.addListener(PufferfishDurability::tick);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
