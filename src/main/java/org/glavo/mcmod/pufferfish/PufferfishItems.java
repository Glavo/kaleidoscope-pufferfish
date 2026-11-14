// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    public static final DeferredItem<Item> COPPER = registerMaterialFish(PufferfishVariant.COPPER.getFirst());
    public static final java.util.List<DeferredItem<Item>> COPPER_VARIANTS = registerCopperFish();
    public static final DeferredItem<Item> IRON = registerMaterialFish(PufferfishVariant.IRON);
    /// An always-edible food with golden apple nutrition and effects, eight minutes of
    /// Water Breathing I, and one minute of Poison II. Right-clicking eats it instead of throwing it.
    public static final DeferredItem<Item> GOLDEN = ITEMS.register("golden_pufferfish", () -> PufferfishVariant.GOLDEN.bind(new Item(
            new Item.Properties().rarity(Rarity.RARE).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(1.2F).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0), 1)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 9600, 0), 1)
                    .effect(() -> new MobEffectInstance(MobEffects.POISON, 1200, 1), 1)
                    .build())) {
        @Override
        public EquipmentSlot getEquipmentSlot(ItemStack stack) { return EquipmentSlot.HEAD; }
    }));
    public static final DeferredItem<Item> DIAMOND = registerMaterialFish(PufferfishVariant.DIAMOND);
    public static final DeferredItem<Item> NETHERITE = ITEMS.register("netherite_pufferfish", () -> PufferfishVariant.NETHERITE.bind(new Item(
            new Item.Properties().fireResistant().attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(PufferfishPlease.id("netherite_head_armor"),
                            3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(PufferfishPlease.id("netherite_head_toughness"),
                            3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .build())) {
        @Override
        public EquipmentSlot getEquipmentSlot(ItemStack stack) {
            return EquipmentSlot.HEAD;
        }
    }));
    /// A fire-resistant mace-strength fish granting head armor, toughness, knockback resistance, and reduced movement speed.
    public static final DeferredItem<HeavyPufferfishItem> HEAVY = ITEMS.register("heavy_pufferfish", () -> PufferfishVariant.HEAVY.bind(new HeavyPufferfishItem(
            new Item.Properties().rarity(Rarity.EPIC).fireResistant().attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(PufferfishPlease.id("heavy_head_armor"),
                            4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(PufferfishPlease.id("heavy_head_toughness"),
                            4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(PufferfishPlease.id("heavy_head_knockback_resistance"),
                            0.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(PufferfishPlease.id("heavy_head_movement_speed"),
                            -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD)
                    .build()))));

    public static final DeferredItem<Item> RADIANT = ITEMS.register("radiant_pufferfish", () -> PufferfishVariant.RADIANT.bind(new Item(
            new Item.Properties().rarity(Rarity.EPIC).fireResistant().attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(PufferfishPlease.id("radiant_head_armor"),
                            3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(PufferfishPlease.id("radiant_head_toughness"),
                            3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .build())) {
        @Override
        public EquipmentSlot getEquipmentSlot(ItemStack stack) {
            return EquipmentSlot.HEAD;
        }
    }));

    /// Edible pufferfish sashimi with two nutrition points and no status effects.
    public static final DeferredItem<Item> SASHIMI = ITEMS.register("pufferfish_sashimi", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build())));
    public static final DeferredItem<Item> RAW_BAOZI = ITEMS.register("raw_pufferfish_baozi", () -> new Item(new Item.Properties()));
    /// A portable meal granting eighty seconds of water breathing before quality scaling.
    public static final DeferredItem<Item> BAOZI = ITEMS.register("pufferfish_baozi", () -> cookedFood(
            new FoodProperties.Builder().nutrition(8).saturationModifier(1).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 80 * 20), 1)));
    /// A bowl meal granting three minutes of water breathing before quality scaling.
    public static final DeferredItem<Item> BRAISED = ITEMS.register("braised_pufferfish", () -> cookedFood(
            new FoodProperties.Builder().nutrition(9).saturationModifier(0.6F).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3 * 60 * 20), 1)
                    .usingConvertsTo(Items.BOWL)));
    /// A rice bowl granting Cookery's satiated shield when the integration is present.
    public static final DeferredItem<Item> BRAISED_RICE_BOWL = ITEMS.register("braised_pufferfish_rice_bowl", () -> {
        var food = new FoodProperties.Builder().nutrition(14).saturationModifier(0.65F).alwaysEdible().usingConvertsTo(Items.BOWL);
        return ModList.get().isLoaded("kaleidoscope_cookery")
                ? org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration.riceBowl(food) : cookedFood(food);
    });
    /// A rice soup retaining Cookery's pufferfish soup mustard effect when available.
    public static final DeferredItem<Item> ZOSUI = ITEMS.register("pufferfish_zosui", () -> {
        var food = new FoodProperties.Builder().nutrition(10).saturationModifier(0.6F).alwaysEdible().usingConvertsTo(Items.BOWL);
        return ModList.get().isLoaded("kaleidoscope_cookery")
                ? org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration.zosui(food) : cookedFood(food);
    });
    public static final DeferredItem<Item> SKIN_BROTH = ITEMS.register("pufferfish_skin_broth", () -> new Item(new Item.Properties().stacksTo(16)) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.pufferfish_please.pufferfish_skin_broth")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });
    /// The reusable flail crafted from an ordinary or dyed pufferfish and two chains.
    public static final DeferredItem<PufferfishFlailItem> FLAIL = ITEMS.register("pufferfish_flail",
            () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1), () -> Items.PUFFERFISH));
    /// The fire-resistant flail crafted from a netherite pufferfish and two chains.
    public static final DeferredItem<PufferfishFlailItem> NETHERITE_FLAIL = ITEMS.register("netherite_pufferfish_flail",
            () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1).fireResistant(), () -> PufferfishItems.NETHERITE.get()));
    public static final DeferredItem<PufferfishFlailItem> HEAVY_FLAIL = ITEMS.register("heavy_pufferfish_flail",
            () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), () -> PufferfishItems.HEAVY.get()));
    public static final DeferredItem<PufferfishFlailItem> RADIANT_FLAIL = ITEMS.register("radiant_pufferfish_flail",
            () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), () -> PufferfishItems.RADIANT.get()));
    public static final java.util.List<DeferredItem<PufferfishFlailItem>> FLAIL_VARIANTS = registerFlails();
    public static final DeferredItem<PufferfishPumpItem> PUMP = ITEMS.register("pufferfish_pump",
            () -> new PufferfishPumpItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SmithingTemplateItem> UPGRADE_TEMPLATE = ITEMS.register("pufferfish_upgrade_smithing_template",
            () -> new SmithingTemplateItem(
                    Component.translatable("item.pufferfish_please.netherite_pufferfish").withStyle(ChatFormatting.BLUE),
                    Component.translatable("item.pufferfish_please.pufferfish_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
                    Component.translatable("upgrade.pufferfish_please.pufferfish_upgrade").withStyle(ChatFormatting.GRAY),
                    Component.translatable("item.pufferfish_please.pufferfish_upgrade.base_slot_description"),
                    Component.translatable("item.pufferfish_please.pufferfish_upgrade.additions_slot_description"),
                    List.of(), List.of()));
    public static final DeferredItem<Item> ANCIENT_PEARL = ITEMS.register("ancient_pufferfish_pearl",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    private PufferfishItems() {}

    /// Uses Cookery's quality-aware meals when installed, otherwise retaining vanilla food behavior.
    private static Item cookedFood(FoodProperties.Builder food) {
        return ModList.get().isLoaded("kaleidoscope_cookery")
                ? org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration.meal(food)
                : new Item(new Item.Properties().food(food.build()));
    }

    private static java.util.List<DeferredItem<Item>> registerCopperFish() {
        var fish = new java.util.ArrayList<DeferredItem<Item>>();
        fish.add(COPPER);
        PufferfishVariant.COPPER.stream().skip(1).map(PufferfishItems::registerMaterialFish).forEach(fish::add);
        return java.util.List.copyOf(fish);
    }

    private static java.util.List<DeferredItem<PufferfishFlailItem>> registerFlails() {
        var flails = new java.util.ArrayList<DeferredItem<PufferfishFlailItem>>();
        flails.add(FLAIL);
        var fish = new java.util.ArrayList<>(COPPER_VARIANTS);
        fish.add(IRON);
        fish.add(GOLDEN);
        fish.add(DIAMOND);
        for (var head : fish) {
            flails.add(ITEMS.register(head.getId().getPath() + "_flail",
                    () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1), head)));
        }
        flails.add(NETHERITE_FLAIL);
        flails.add(HEAVY_FLAIL);
        flails.add(RADIANT_FLAIL);
        return java.util.List.copyOf(flails);
    }

    public static ItemStack flailFish(ItemStack stack) {
        return stack.getItem() instanceof PufferfishFlailItem flail ? flail.fish(stack) : stack;
    }

    private static DeferredItem<Item> registerMaterialFish(PufferfishVariant variant) {
        return ITEMS.register(variant.fishName(), () -> variant.bind(new Item(new Item.Properties()) {
            @Override
            public EquipmentSlot getEquipmentSlot(ItemStack stack) {
                return EquipmentSlot.HEAD;
            }
        }));
    }

    @org.jetbrains.annotations.Nullable
    public static String materialName(ItemStack stack) {
        if (!isPufferfish(stack)) return null;
        var variant = PufferfishVariant.of(stack);
        return switch (variant.material()) {
            case COPPER -> CopperPufferfish.material(stack);
            case IRON, GOLDEN, DIAMOND, RADIANT -> variant.fishName().replace("_pufferfish", "");
            default -> null;
        };
    }

    public static boolean isRadiant(ItemStack stack) {
        var variant = PufferfishVariant.of(stack);
        return variant != null && variant.material() == PufferfishMaterial.RADIANT;
    }

    /// Returns whether the stack contains a heavy pufferfish.
    public static boolean isHeavy(ItemStack stack) {
        return stack.is(HEAVY.get());
    }

    public static boolean isNetherite(ItemStack stack) {
        return stack.is(NETHERITE.get());
    }

    public static boolean isPufferfish(ItemStack stack) {
        var variant = PufferfishVariant.of(stack);
        return variant != null && stack.getItem() == variant.fishItem();
    }

    public static boolean canThrow(ItemStack stack) {
        return isPufferfish(stack) && !stack.is(GOLDEN.get());
    }

    /// Returns whether the stack is a flail variant.
    public static boolean isFlail(ItemStack stack) {
        return stack.getItem() instanceof PufferfishFlailItem;
    }

    public static boolean hasHeavyCombat(ItemStack stack) {
        return isHeavy(stack) || stack.is(HEAVY_FLAIL.get());
    }

    /// Returns whether the stack is a fish or flail with netherite combat properties.
    public static boolean hasNetheriteCombat(ItemStack stack) {
        return isNetherite(stack) || stack.is(RADIANT.get()) || hasHeavyCombat(stack) || stack.is(NETHERITE_FLAIL.get()) || stack.is(RADIANT_FLAIL.get());
    }

    /// Returns whether the stack supports pufferfish melee damage and poison.
    public static boolean isWeapon(ItemStack stack) {
        return isPufferfish(stack) || isFlail(stack);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
