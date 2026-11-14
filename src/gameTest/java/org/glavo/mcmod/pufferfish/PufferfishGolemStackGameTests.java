// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import org.glavo.mcmod.pufferfish.test.CuttingAssertions;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishGolemStackGameTests {
    @GameTest(template = "empty")
    public static void naturalTowersRespectSpawnReasonExistingEquipmentAndReloads(GameTestHelper helper) {
        for (var reason : MobSpawnType.values()) {
            var random = new TowerRandom();
            var golem = naturalGolem(helper, random);
            random.rolls(0);
            finalizeGolem(helper, golem, reason);
            boolean expected = reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION
                    || reason == MobSpawnType.STRUCTURE || reason == MobSpawnType.MOB_SUMMONED;
            helper.assertTrue(PufferfishHeadgear.isWearing(golem) == expected, "World generation did not initialize headgear before saving");
            helper.getLevel().addFreshEntity(golem);
            helper.assertTrue(PufferfishHeadgear.isWearing(golem) == expected, "Wrong headgear spawn origin: " + reason);
            golem.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(golem, helper.getLevel(), true));
            NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(golem, helper.getLevel()));
            helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Reload or repeated join rerolled headgear");
            golem.discard();
        }
        for (int scenario = 0; scenario < 4; scenario++) {
            var random = new TowerRandom();
            var golem = naturalGolem(helper, random);
            random.rolls(0);
            if (scenario == 0) golem.setPlayerCreated(true);
            if (scenario == 1) golem.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            if (scenario == 2) golem.setData(PufferfishPlease.GOLEM_FISH, List.of(new ItemStack(PufferfishItems.IRON.get())));
            var head = golem.getItemBySlot(EquipmentSlot.HEAD);
            var decorations = PufferfishGolemStack.decorations(golem);
            if (scenario != 3) finalizeGolem(helper, golem, MobSpawnType.STRUCTURE);
            NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(golem, helper.getLevel(), scenario == 3));
            helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD) == head && PufferfishGolemStack.decorations(golem).equals(decorations),
                    "Natural headgear replaced equipment, affected a player-built golem or initialized a saved entity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalTowerChanceAndSizeRespectRollBoundaries(GameTestHelper helper) {
        for (int chance = 0; chance < 10; chance++) {
            var random = new TowerRandom();
            var golem = naturalGolem(helper, random);
            random.fraction = chance / 10.0;
            random.rolls(0);
            finalizeGolem(helper, golem, MobSpawnType.MOB_SUMMONED);
            NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(golem, helper.getLevel()));
            helper.assertTrue(PufferfishHeadgear.isWearing(golem) == (chance == 0), "Golem headgear chance was not one in ten");
        }
        for (int roll = 0; roll < 100; roll++) {
            for (boolean maximum : new boolean[] {false, true}) {
                var random = new TowerRandom();
                var golem = naturalGolem(helper, random);
                int offset = roll < 99 ? (maximum ? 2 : 0) : (maximum ? 11 : 0);
                if (roll < 90) random.rolls(roll);
                else random.rolls(roll, offset);
                finalizeGolem(helper, golem, MobSpawnType.STRUCTURE);
                NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(golem, helper.getLevel()));
                int expected = roll < 90 ? 1 : roll < 99 ? 2 + offset : 5 + offset;
                helper.assertTrue(PufferfishHeadgear.isWearing(golem) && PufferfishGolemStack.decorations(golem).size() + 1 == expected,
                        "Natural tower size did not follow the 90/9/1 distribution");
                helper.assertTrue(!golem.getItemBySlot(EquipmentSlot.HEAD).has(PufferfishPlease.CULTIVATION.get())
                        && PufferfishGolemStack.decorations(golem).stream().noneMatch(fish -> fish.has(PufferfishPlease.CULTIVATION.get())),
                        "Natural tower generated ranked fish");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalTowerFishMatchOceanWeightsAndHaveNoCultivation(GameTestHelper helper) {
        var pool = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.WARM_OCEAN).value().getMobSettings()
                .getMobs(net.minecraft.world.entity.MobCategory.WATER_AMBIENT).unwrap();
        var expected = new java.util.LinkedHashMap<net.minecraft.world.item.Item, Double>();
        for (var entry : pool) {
            if (entry.type == EntityType.PUFFERFISH) expected.put(Items.PUFFERFISH, (double) entry.getWeight().asInt());
            else for (var variant : PufferfishMobs.VARIANTS.values()) {
                if (variant.entity().get() != entry.type) continue;
                var fish = variant.fish().get();
                int divisor = fish == PufferfishItems.RADIANT.get() || fish == PufferfishItems.HEAVY.get() ? 256
                        : fish == PufferfishItems.NETHERITE.get() ? 64 : fish == PufferfishItems.DIAMOND.get() ? 16
                        : fish == PufferfishItems.GOLDEN.get() ? 8 : 4;
                expected.put(fish, entry.getWeight().asInt() / (double) divisor);
                helper.assertTrue(!variant.fish().getId().getPath().startsWith("waxed_"), "Waxed fish entered the natural pool");
            }
        }
        double total = expected.values().stream().mapToDouble(Double::doubleValue).sum();
        double before = 0;
        var random = new TowerRandom();
        for (var entry : expected.entrySet()) {
            random.fraction = (before + entry.getValue() / 2) / total;
            for (boolean dyed : new boolean[] {false, true}) {
                random.rolls(dyed ? 0 : 1, 5);
                var fish = PufferfishMobs.naturalFish(helper.getLevel().registryAccess(), random);
                helper.assertTrue(fish.is(entry.getKey()) && fish.getCount() == 1 && !fish.has(PufferfishPlease.CULTIVATION.get()),
                        "Natural headgear selection changed rarity, material or rank");
                if (fish.is(Items.PUFFERFISH)) helper.assertTrue(PufferfishColors.get(fish)
                        == (dyed ? net.minecraft.world.item.DyeColor.LIME : null), "Natural headgear color did not follow the fish color roll");
            }
            before += entry.getValue();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalTowerConfigurationControlsChanceWeightsAndCounts(GameTestHelper helper) {
        var chance = PufferfishMobConfig.GOLEM_HEADGEAR_CHANCE;
        var single = PufferfishMobConfig.GOLEM_SINGLE_WEIGHT;
        var small = PufferfishMobConfig.GOLEM_SMALL_WEIGHT;
        var large = PufferfishMobConfig.GOLEM_LARGE_WEIGHT;
        var settings = List.of(single, small, large, PufferfishMobConfig.GOLEM_SMALL_MIN_COUNT,
                PufferfishMobConfig.GOLEM_SMALL_MAX_COUNT, PufferfishMobConfig.GOLEM_LARGE_MIN_COUNT,
                PufferfishMobConfig.GOLEM_LARGE_MAX_COUNT, PufferfishMobConfig.GOLEM_MAX_STACK_SIZE);
        var previous = settings.stream().map(setting -> setting.get()).toList();
        double previousChance = chance.get();
        try {
            single.set(1);
            small.set(0);
            large.set(0);
            for (double probability : new double[] {0, 0.35, 1}) for (double roll : new double[] {0, 0.349, 0.35, 0.999}) {
                chance.set(probability);
                var random = new TowerRandom();
                random.fraction = roll;
                var golem = naturalGolem(helper, random);
                finalizeGolem(helper, golem, MobSpawnType.STRUCTURE);
                helper.assertTrue(PufferfishHeadgear.isWearing(golem) == (roll < probability), "Configured headgear chance was ignored");
            }
            chance.set(1.0);
            PufferfishMobConfig.GOLEM_SMALL_MIN_COUNT.set(7);
            PufferfishMobConfig.GOLEM_SMALL_MAX_COUNT.set(7);
            PufferfishMobConfig.GOLEM_LARGE_MIN_COUNT.set(11);
            PufferfishMobConfig.GOLEM_LARGE_MAX_COUNT.set(11);
            for (int group = 0; group < 4; group++) {
                single.set(group == 0 ? 1 : 0);
                small.set(group == 1 ? 1 : 0);
                large.set(group == 2 ? 1 : 0);
                var golem = naturalGolem(helper, new TowerRandom());
                finalizeGolem(helper, golem, MobSpawnType.MOB_SUMMONED);
                int expected = switch (group) { case 0 -> 1; case 1 -> 7; case 2 -> 11; default -> 0; };
                helper.assertTrue(PufferfishGolemStack.decorations(golem).size() + (PufferfishHeadgear.isWearing(golem) ? 1 : 0) == expected,
                        "Configured tower weights or ranges were ignored");
            }
            large.set(1);
            PufferfishMobConfig.GOLEM_LARGE_MIN_COUNT.set(13);
            PufferfishMobConfig.GOLEM_LARGE_MAX_COUNT.set(11);
            var reversed = naturalGolem(helper, new TowerRandom());
            finalizeGolem(helper, reversed, MobSpawnType.STRUCTURE);
            helper.assertTrue(PufferfishGolemStack.decorations(reversed).size() == 12, "Reversed count endpoints were not supported");
            PufferfishMobConfig.GOLEM_MAX_STACK_SIZE.set(1);
            var capped = naturalGolem(helper, new TowerRandom());
            finalizeGolem(helper, capped, MobSpawnType.STRUCTURE);
            helper.assertTrue(PufferfishHeadgear.isWearing(capped) && PufferfishGolemStack.decorations(capped).isEmpty(),
                    "Natural tower exceeded the configured stack limit");
        } finally {
            chance.set(previousChance);
            for (int i = 0; i < settings.size(); i++) settings.get(i).set(previous.get(i));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void changedStackLimitPreservesExistingTowersAndAllowsRetrieval(GameTestHelper helper) {
        var limit = PufferfishMobConfig.GOLEM_MAX_STACK_SIZE;
        int previous = limit.get();
        try {
            var player = TestPlayers.serverPlayer(helper);
            player.setShiftKeyDown(true);
            var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
            limit.set(1024);
            for (int i = 0; i < 1024; i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
                player.interactOn(golem, InteractionHand.MAIN_HAND);
                helper.assertTrue(player.getMainHandItem().isEmpty(), "Raised stack limit rejected a fish");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
            player.interactOn(golem, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Full tower consumed an extra fish");
            limit.set(1);
            var saved = new CompoundTag();
            golem.saveWithoutId(saved);
            var restored = EntityType.IRON_GOLEM.create(helper.getLevel());
            restored.load(saved);
            var receiver = EntityType.IRON_GOLEM.create(helper.getLevel());
            var tracker = new ServerEntity(helper.getLevel(), restored, 3, true, packet -> {});
            tracker.sendPairingData(FakePlayerFactory.getMinecraft(helper.getLevel()), new PacketAndPayloadAcceptor<>(packet -> {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SyncAttachmentsPayload data) {
                    AttachmentSync.receiveSyncedDataAttachments(receiver, helper.getLevel().registryAccess(), data.types(), data.syncPayload());
                }
            }));
            helper.assertTrue(PufferfishGolemStack.decorations(receiver).size() == 1023, "Lowered limit truncated saved or synchronized fish");
            player.interactOn(restored, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Lowered limit allowed another fish");
            for (int i = 0; i < 1024; i++) {
                player.getInventory().clearContent();
                player.interactOn(restored, InteractionHand.MAIN_HAND);
                helper.assertTrue(player.getMainHandItem().is(Items.PUFFERFISH) && player.getMainHandItem().getCount() == 1,
                        "Lowered limit prevented retrieval or lost a fish");
            }
            helper.assertTrue(restored.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && PufferfishGolemStack.decorations(restored).isEmpty(),
                    "Retrieval duplicated tower fish");
            player.interactOn(restored, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().isEmpty() && PufferfishHeadgear.isWearing(restored),
                    "A stack limit of one prevented equipping the first fish");
        } finally {
            limit.set(previous);
        }
        helper.succeed();
    }

    private static IronGolem naturalGolem(GameTestHelper helper, TowerRandom source) {
        var golem = new IronGolem(EntityType.IRON_GOLEM, helper.getLevel()) {
            @Override public net.minecraft.util.RandomSource getRandom() { return source; }
        };
        golem.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), 0, 0);
        return golem;
    }

    private static void finalizeGolem(GameTestHelper helper, IronGolem golem, MobSpawnType reason) {
        EventHooks.finalizeMobSpawn(golem, helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(golem.blockPosition()), reason, null);
    }

    private static final class TowerRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        private final java.util.ArrayDeque<Integer> rolls = new java.util.ArrayDeque<>();
        private double fraction;

        private TowerRandom() { super(0); }

        private void rolls(int... values) {
            rolls.clear();
            for (int value : values) rolls.add(value);
        }

        @Override public double nextDouble() { return fraction; }

        @Override public double nextGaussian() { return 0; }

        @Override public int nextInt(int bound) {
            int value = rolls.isEmpty() ? bound - 1 : rolls.removeFirst();
            if (value < 0 || value >= bound) throw new AssertionError("Tower roll exceeded bound " + bound);
            return value;
        }
    }

    @GameTest(template = "empty")
    public static void mixedTowerPreservesOrderCapacityAndTracking(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setShiftKeyDown(true);
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        var expected = new ArrayList<ItemStack>();
        var variants = new ArrayList<>(PufferfishVariant.ALL);
        for (int i = 0; i < 256; i++) {
            var fish = CuttingAssertions.rankedFish(variants.get(i % variants.size()), i % 11);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Tower fish " + i));
            PufferfishDurability.activate(fish);
            fish.setDamageValue(i % 7);
            expected.add(fish.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, fish);
            helper.assertTrue(player.interactOn(golem, InteractionHand.MAIN_HAND).consumesAction()
                    && player.getMainHandItem().isEmpty(), "Tower did not consume one fish");
        }
        helper.assertTrue(ItemStack.matches(golem.getItemBySlot(EquipmentSlot.HEAD), expected.getFirst()), "Stacking replaced the active fish");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && PufferfishGolemStack.decorations(golem).size() == 255,
                "Full tower consumed an item or exceeded capacity");
        var saved = new CompoundTag();
        golem.saveWithoutId(saved);
        var restored = EntityType.IRON_GOLEM.create(helper.getLevel());
        restored.load(saved);
        var receiver = EntityType.IRON_GOLEM.create(helper.getLevel());
        var tracker = new ServerEntity(helper.getLevel(), restored, 3, true, packet -> {});
        tracker.sendPairingData(FakePlayerFactory.getMinecraft(helper.getLevel()), new PacketAndPayloadAcceptor<>(packet -> {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SyncAttachmentsPayload data) {
                AttachmentSync.receiveSyncedDataAttachments(receiver, helper.getLevel().registryAccess(), data.types(), data.syncPayload());
            }
        }));
        helper.assertTrue(PufferfishGolemStack.decorations(receiver).size() == 255, "Tracking lost the tower");
        for (int i = 1; i < expected.size(); i++) {
            helper.assertTrue(ItemStack.matches(PufferfishGolemStack.decorations(receiver).get(i - 1), expected.get(i)),
                    "Tracking changed fish order or components");
        }
        for (int i = expected.size() - 1; i >= 0; i--) {
            player.getInventory().clearContent();
            helper.assertTrue(player.interactOn(restored, InteractionHand.MAIN_HAND).consumesAction()
                    && ItemStack.matches(player.getMainHandItem(), expected.get(i)), "Retrieval lost fish data or order");
        }
        helper.assertTrue(restored.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !restored.hasData(PufferfishPlease.GOLEM_FISH),
                "Empty tower retained fish or attachment data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyBottomFishActsAndNextFishReplacesBrokenHeadgear(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setShiftKeyDown(true);
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        for (var stack : List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(PufferfishItems.RADIANT.get()))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.interactOn(golem, InteractionHand.MAIN_HAND);
        }
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(golem));
        helper.assertTrue(!golem.hasEffect(MobEffects.NIGHT_VISION), "Decoration granted an equipment effect");
        var head = golem.getItemBySlot(EquipmentSlot.HEAD);
        PufferfishDurability.activate(head);
        head.setDamageValue(head.getMaxDamage() - 1);
        var attacker = EntityType.PIG.create(helper.getLevel());
        golem.hurt(golem.damageSources().mobAttack(attacker), 1);
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Active headgear did not break");
        helper.assertTrue(PufferfishGolemStack.decorations(golem).getFirst().getDamageValue() == 0,
                "Decoration took equipment wear");
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(golem));
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(golem));
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).is(PufferfishItems.RADIANT.get())
                && PufferfishGolemStack.decorations(golem).isEmpty() && golem.hasEffect(MobEffects.NIGHT_VISION),
                "Next fish did not replace broken headgear and gain its effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerRetrievalRespectsBindingAndForeignEquipment(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setShiftKeyDown(true);
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        golem.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && !golem.hasData(PufferfishPlease.GOLEM_FISH),
                "Tower overwrote foreign headgear");
        golem.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        var top = new ItemStack(PufferfishItems.IRON.get());
        top.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, top);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().isEmpty() && PufferfishGolemStack.decorations(golem).size() == 1,
                "Survival retrieval bypassed the top fish's binding curse");
        player.setGameMode(GameType.CREATIVE);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(PufferfishItems.IRON.get()) && PufferfishGolemStack.decorations(golem).isEmpty()
                && golem.getItemBySlot(EquipmentSlot.HEAD).is(Items.PUFFERFISH), "Creative retrieval removed the wrong fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerDeathDropsRespectVanishingAndMobLoot(GameTestHelper helper) {
        var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        boolean previous = rule.get();
        try {
            for (boolean mobLoot : new boolean[] {true, false}) {
                rule.set(mobLoot, helper.getLevel().getServer());
                var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
                var player = TestPlayers.serverPlayer(helper);
                player.setShiftKeyDown(true);
                var expected = new ArrayList<ItemStack>();
                for (int i = 0; i < 4; i++) {
                    var fish = new ItemStack(Items.PUFFERFISH);
                    PufferfishDurability.activate(fish);
                    fish.set(DataComponents.CUSTOM_NAME, Component.literal("Death drop " + i));
                    if (i == 2) fish.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.VANISHING_CURSE), 1);
                    else expected.add(fish.copy());
                    player.setItemInHand(InteractionHand.MAIN_HAND, fish);
                    player.interactOn(golem, InteractionHand.MAIN_HAND);
                }
                var drops = new ArrayList<ItemStack>();
                Consumer<LivingDropsEvent> listener = event -> {
                    if (event.getEntity() == golem) {
                        event.getDrops().stream().map(item -> item.getItem()).filter(PufferfishItems::isPufferfish).forEach(drops::add);
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, listener);
                try {
                    golem.hurt(golem.damageSources().genericKill(), 1000);
                } finally {
                    NeoForge.EVENT_BUS.unregister(listener);
                }
                helper.assertTrue(drops.size() == (mobLoot ? expected.size() : 0), "Tower drops ignored mob loot or vanishing");
                if (mobLoot) for (var fish : expected) helper.assertTrue(drops.stream().anyMatch(drop -> ItemStack.matches(drop, fish)),
                        "Death lost fish components or duplicated a fish");
                helper.assertTrue(PufferfishGolemStack.decorations(golem).isEmpty(), "Death retained tower items");
            }
        } finally {
            rule.set(previous, helper.getLevel().getServer());
        }
        helper.succeed();
    }
}
