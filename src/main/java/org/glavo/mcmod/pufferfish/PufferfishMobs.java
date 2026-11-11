// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

public final class PufferfishMobs {
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, PufferfishPlease.id("pufferfish_food"));
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PufferfishPlease.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    public static final Map<String, Variant> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, Variant>();
        for (int i = 0; i < PufferfishItems.COPPER_VARIANTS.size(); i++) {
            add(variants, PufferfishItems.COPPER_VARIANTS.get(i), i % 4 == 0 ? 0xC5724B : 0x58B69C, 0x653322, 4);
        }
        add(variants, PufferfishItems.IRON, 0xCDD2D3, 0x73787F, 4);
        add(variants, PufferfishItems.GOLDEN, 0xFAD64A, 0xB26411, 8);
        add(variants, PufferfishItems.DIAMOND, 0x6EE1D6, 0x258C91, 16, 64);
        add(variants, PufferfishItems.NETHERITE, 0x594F52, 0xE5DED1, 64, 1024);
        add(variants, PufferfishItems.HEAVY, 0x73787F, 0xE5F0FA, 256, 4096);
        add(variants, PufferfishItems.RADIANT, 0x9A92AE, 0xFFBE20, 256, 4096);
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishMobs() {}

    private static void add(Map<String, Variant> variants, DeferredItem<? extends Item> fish, int shell, int spots, int rarity) {
        add(variants, fish, shell, spots, rarity, rarity);
    }

    private static void add(Map<String, Variant> variants, DeferredItem<? extends Item> fish, int shell, int spots, int rarity, int conduitRarity) {
        String name = fish.getId().getPath();
        var entity = ENTITIES.register(name, () -> {
            var builder = EntityType.Builder.<MaterialPufferfish>of((type, level) -> new MaterialPufferfish(type, level, fish.get()), MobCategory.WATER_AMBIENT)
                    .sized(0.7F, 0.7F).eyeHeight(0.455F).clientTrackingRange(4);
            if (name.startsWith("netherite_") || name.startsWith("radiant_") || name.startsWith("heavy_")) builder.fireImmune();
            return builder.build(PufferfishPlease.id(name).toString());
        });
        var bucket = ITEMS.register(name + "_bucket", () -> PufferfishVariant.of(new ItemStack(fish.get())).bind(new MobBucketItem(entity.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH,
                new Item.Properties().stacksTo(1))));
        var egg = ITEMS.register(name + "_spawn_egg", () -> PufferfishVariant.of(new ItemStack(fish.get())).bind(new DeferredSpawnEggItem(entity, shell, spots, new Item.Properties())));
        variants.put(name, new Variant(fish, entity, bucket, egg, rarity, conduitRarity));
    }

    public static Variant variant(ItemStack fish) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(fish.getItem());
        return id.getNamespace().equals(PufferfishPlease.MODID) ? VARIANTS.get(id.getPath()) : null;
    }

    public static ItemStack dyedEgg(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH_SPAWN_EGG);
        stack.set(PufferfishPlease.COLOR.get(), color);
        stack.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable(
                "item.pufferfish_please.dyed_pufferfish_spawn_egg", net.minecraft.network.chat.Component.translatable("color.minecraft." + color.getName())));
        return stack;
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(PufferfishMobs::attributes);
        bus.addListener(PufferfishMobs::placements);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                VARIANTS.values().forEach(variant -> net.minecraft.world.level.block.DispenserBlock.registerBehavior(variant.bucket().get(),
                        net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(Items.PUFFERFISH_BUCKET)))));
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        VARIANTS.values().forEach(variant -> event.put(variant.entity().get(), PufferfishMobConfig.attributes(variant.fish().toStack())));
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        VARIANTS.values().forEach(variant -> event.register(variant.entity().get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) ->
                        WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, reason, pos, random)
                                && (reason != MobSpawnType.NATURAL || random.nextInt(PufferfishConduitSpawning.isInRange(level.getLevel(), pos)
                                        ? variant.conduitRarity() : variant.rarity()) == 0),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    public static void naturalColors(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL && event.getEntity().getType() == EntityType.PUFFERFISH
                && event.getEntity() instanceof Pufferfish fish && fish.getRandom().nextInt(4) == 0) {
            PufferfishColors.set(fish, DyeColor.byId(fish.getRandom().nextInt(16)));
        }
    }

    public static void feed(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!(event.getTarget() instanceof Pufferfish fish) || !fish.isAlive() || fish.getHealth() >= fish.getMaxHealth()
                || player.isSpectator() || !player.mayBuild()) return;
        var stack = event.getItemStack();
        if (!stack.is(FOOD)) return;
        var food = stack.getFoodProperties(fish);
        if (food == null || food.nutrition() <= 0) return;
        if (fish.level() instanceof ServerLevel level) {
            var crumbs = new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1));
            fish.heal(food.nutrition());
            fish.setPersistenceRequired();
            if (!player.hasInfiniteMaterials()) {
                var remainder = food.usingConvertsTo().orElseGet(stack::getCraftingRemainingItem).copy();
                player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(stack, player, remainder));
            }
            fish.playSound(SoundEvents.GENERIC_EAT, 1, 1);
            fish.gameEvent(GameEvent.EAT);
            var mouth = fish.getEyePosition().add(fish.getLookAngle().scale(fish.getBbWidth() * 0.5));
            level.sendParticles(crumbs, mouth.x, mouth.y, mouth.z, 5, 0.08, 0.08, 0.08, 0.02);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(fish.level().isClientSide));
        event.setCanceled(true);
    }

    public record Variant(DeferredItem<? extends Item> fish, DeferredHolder<EntityType<?>, EntityType<MaterialPufferfish>> entity,
            DeferredItem<MobBucketItem> bucket, DeferredItem<DeferredSpawnEggItem> egg, int rarity, int conduitRarity) {}
}
