// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestPlayers;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;
import org.glavo.mcmod.pufferfish.worldgen.PufferfishRuinPiece;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishAdvancementGameTests {
    @GameTest(template = "empty")
    public static void weaponProgressRequiresADirectHostileKill(GameTestHelper helper) {
        var players = new java.util.LinkedHashMap<String, ServerPlayer>();
        for (var mode : List.of("melee", "throw", "flail", "sword", "thorns", "poison", "wither", "passive", "canceled")) {
            var player = player(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, mode.equals("melee") ? new ItemStack(Items.PUFFERFISH) : PufferfishItems.IRON.toStack());
            var target = mode.equals("passive") ? helper.spawn(EntityType.COW, new BlockPos(2, 3, 4))
                    : helper.spawn(EntityType.CREEPER, new BlockPos(2, 3, 4));
            target.setNoAi(true);
            target.setNoGravity(true);
            target.setHealth(0.1F);
            switch (mode) {
                case "throw" -> {
                    var fish = new ThrownPufferfish(helper.getLevel(), player);
                    fish.setItem(PufferfishVariant.DYED.get(DyeColor.BLUE).fish());
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    fish.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
                    fish.setDeltaMovement(0, 0, 3);
                    helper.getLevel().addFreshEntity(fish);
                    fish.tick();
                    fish.discard();
                }
                case "flail" -> {
                    player.setItemInHand(InteractionHand.MAIN_HAND, PufferfishItems.HEAVY_FLAIL.toStack());
                    var head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    head.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
                    head.setDeltaMovement(0, 0, 3);
                    helper.getLevel().addFreshEntity(head);
                    head.tick();
                    head.discard();
                }
                case "thorns" -> target.hurt(helper.getLevel().damageSources().thorns(player), 10);
                case "poison", "wither" -> {
                    target.setLastHurtByPlayer(player);
                    target.hurt(mode.equals("poison") ? helper.getLevel().damageSources().magic()
                            : helper.getLevel().damageSources().wither(), 10);
                }
                case "canceled" -> {
                    java.util.function.Consumer<LivingDeathEvent> cancel = event -> {
                        if (event.getEntity() == target) {
                            event.setCanceled(true);
                            target.setHealth(1);
                        }
                    };
                    NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, cancel);
                    try {
                        player.attack(target);
                    } finally {
                        NeoForge.EVENT_BUS.unregister(cancel);
                    }
                }
                default -> {
                    if (mode.equals("sword")) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    player.attack(target);
                }
            }
            helper.assertTrue(target.isDeadOrDying() != mode.equals("canceled"), "Unexpected death outcome: " + mode);
            players.put(mode, player);
        }
        helper.runAfterDelay(2, () -> {
            players.forEach((mode, player) -> {
                boolean granted = List.of("melee", "throw", "flail").contains(mode);
                helper.assertTrue(done(player, "fish_fighter") == granted, "Incorrect weapon kill progress: " + mode);
                helper.assertTrue(!done(player, "tetanus"), "Unrelated or indirect damage granted tetanus progress: " + mode);
                player.getAdvancements().stopListening();
            });
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void tetanusUsesTheKillingWeaponAcrossFishAndFlailVariants(GameTestHelper helper) {
        record KillCase(ServerPlayer player, boolean tetanus, String name) {}
        var cases = new java.util.ArrayList<KillCase>();
        var weapons = new java.util.ArrayList<ItemStack>();
        PufferfishVariant.ALL.forEach(variant -> weapons.add(variant.fish()));
        PufferfishItems.FLAIL_VARIANTS.forEach(item -> weapons.add(item.toStack()));
        for (var weapon : weapons) {
            var fish = PufferfishItems.flailFish(weapon);
            boolean expected = fish.is(PufferfishItems.IRON.get())
                    || PufferfishItems.COPPER_VARIANTS.stream().anyMatch(item -> fish.is(item.get()));
            for (boolean projectile : new boolean[] {false, true}) {
                if (!projectile && PufferfishItems.isFlail(weapon)) continue;
                if (projectile && !PufferfishItems.isFlail(weapon) && !PufferfishItems.canThrow(weapon)) continue;
                var player = player(helper);
                player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
                var target = helper.spawn(EntityType.CREEPER, new BlockPos(2, 3, 4));
                target.setNoAi(true);
                target.setNoGravity(true);
                target.setHealth(0.1F);
                if (projectile) {
                    net.minecraft.world.entity.projectile.ThrowableItemProjectile shot;
                    if (PufferfishItems.isFlail(weapon)) {
                        shot = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    } else {
                        shot = new ThrownPufferfish(helper.getLevel(), player);
                        shot.setItem(weapon.copy());
                        player.setItemInHand(InteractionHand.MAIN_HAND, expected ? new ItemStack(Items.PUFFERFISH) : PufferfishItems.IRON.toStack());
                    }
                    shot.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
                    shot.setDeltaMovement(0, 0, 3);
                    helper.getLevel().addFreshEntity(shot);
                    shot.tick();
                    shot.discard();
                } else {
                    player.attack(target);
                }
                String name = PufferfishVariant.of(weapon).fishName() + (projectile ? " projectile" : " melee");
                helper.assertTrue(target.isDeadOrDying(), "Weapon did not kill its target: " + name);
                // The deferred death check must retain the weapon that actually killed the target.
                player.setItemInHand(InteractionHand.MAIN_HAND, expected ? new ItemStack(Items.PUFFERFISH) : PufferfishItems.IRON.toStack());
                cases.add(new KillCase(player, expected, name));
                target.discard();
            }
        }
        helper.runAfterDelay(2, () -> {
            for (var test : cases) {
                helper.assertTrue(done(test.player(), "fish_fighter"), "Weapon kill did not grant the parent: " + test.name());
                helper.assertTrue(done(test.player(), "tetanus") == test.tetanus(), "Incorrect material kill progress: " + test.name());
                test.player().getAdvancements().stopListening();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void heavyFishAndBothFallingBlocksGrantTheDeathAdvancement(GameTestHelper helper) {
        var level = helper.getLevel();
        var fish = PufferfishMobs.VARIANTS.get("heavy_pufferfish").entity().get().create(level);
        var players = new java.util.ArrayList<ServerPlayer>();
        var victim = player(helper);
        victim.hurt(level.damageSources().mobAttack(fish), 100);
        helper.assertTrue(victim.isDeadOrDying(), "Heavy pufferfish attack did not kill the player");
        players.add(victim);
        for (var block : List.of(PufferfishBlocks.PLACED_HEAVY_FISH.get(), PufferfishLamps.VARIANTS.get("heavy_pufferfish_lamp").block().get())) {
            var falling = FallingBlockEntity.fall(level, helper.absolutePos(new BlockPos(2, 6, 2)), block.defaultBlockState());
            var player = player(helper);
            player.hurt(((HeavyPufferfishBlock) block).getFallDamageSource(falling), 100);
            helper.assertTrue(player.isDeadOrDying(), "Falling heavy pufferfish did not kill the player");
            players.add(player);
            falling.discard();
        }
        helper.runAfterDelay(2, () -> {
            for (var player : players) {
                helper.assertTrue(done(player, "unbearable_weight"), "Heavy pufferfish death did not grant progress");
                player.getAdvancements().stopListening();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void unrelatedDeathsDoNotGrantTheHeavyFishAdvancement(GameTestHelper helper) {
        var level = helper.getLevel();
        var iron = PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get().create(level);
        var anvil = FallingBlockEntity.fall(level, helper.absolutePos(new BlockPos(2, 6, 2)), Blocks.ANVIL.defaultBlockState());
        var players = new java.util.ArrayList<ServerPlayer>();
        for (var source : List.of(level.damageSources().mobAttack(iron), level.damageSources().anvil(anvil),
                new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(PufferfishContactDamage.DAMAGE_TYPE)))) {
            var player = player(helper);
            player.hurt(source, 100);
            helper.assertTrue(player.isDeadOrDying(), "Negative case did not kill the player");
            players.add(player);
        }
        anvil.discard();
        helper.runAfterDelay(2, () -> {
            for (var player : players) {
                helper.assertTrue(!done(player, "unbearable_weight"), "Unrelated death granted heavy pufferfish progress");
                player.getAdvancements().stopListening();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void survivingHeavyFishDamageDoesNotGrantDeathProgress(GameTestHelper helper) {
        var level = helper.getLevel();
        var fish = PufferfishMobs.VARIANTS.get("heavy_pufferfish").entity().get().create(level);
        var source = level.damageSources().mobAttack(fish);
        var injured = player(helper);
        injured.hurt(source, 1);
        helper.assertTrue(injured.getHealth() < injured.getMaxHealth() && !injured.isDeadOrDying(), "Nonlethal attack did not injure the player");
        var saved = player(helper);
        saved.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        saved.hurt(source, 100);
        helper.assertTrue(!saved.isDeadOrDying() && saved.getOffhandItem().isEmpty(), "Totem did not save the player");
        var canceled = player(helper);
        java.util.function.Consumer<LivingDeathEvent> cancelDeath = event -> {
            if (event.getEntity() == canceled) {
                event.setCanceled(true);
                canceled.setHealth(1);
            }
        };
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, cancelDeath);
        try {
            canceled.hurt(source, 100);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancelDeath);
        }
        helper.assertTrue(!canceled.isDeadOrDying(), "Death cancellation did not save the player");
        helper.runAfterDelay(2, () -> {
            for (var player : List.of(injured, saved, canceled)) {
                helper.assertTrue(!done(player, "unbearable_weight"), "Surviving a heavy pufferfish granted death progress");
                player.getAdvancements().stopListening();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ordinaryAndDyedCapturesGrantOnlyTheFirstTrainerRank(GameTestHelper helper) {
        for (int color = -1; color < DyeColor.values().length; color++) {
            var player = player(helper);
            var fish = EntityType.PUFFERFISH.create(helper.getLevel());
            if (color >= 0) PufferfishColors.set(fish, DyeColor.byId(color));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            fish.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(fish.isRemoved() && player.getMainHandItem().is(Items.PUFFERFISH_BUCKET), "Pufferfish was not captured");
            helper.assertTrue(done(player, "root") && done(player, "pufferfish_trainer"), "Capture did not unlock the root and trainer rank");
            helper.assertTrue(!done(player, "experienced_pufferfish_trainer") && !done(player, "legendary_pufferfish_trainer"),
                    "Ordinary or dyed pufferfish granted a higher trainer rank");
            player.getAdvancements().stopListening();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialCapturesGrantTheirRankAndAllLowerRanks(GameTestHelper helper) {
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var player = player(helper);
            var fish = variant.entity().get().create(helper.getLevel());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            if (!fish.canCapture()) {
                fish.interact(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(!fish.isRemoved() && player.getMainHandItem().is(Items.WATER_BUCKET), "Healthy fish was captured");
                helper.assertTrue(!done(player, "root") && !done(player, "pufferfish_trainer")
                                && !done(player, "experienced_pufferfish_trainer") && !done(player, "legendary_pufferfish_trainer"),
                        "Failed capture granted progress");
            }
            fish.setHealth(1);
            fish.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(fish.isRemoved() && player.getMainHandItem().is(variant.bucket().get()), "Material fish was not captured");
            helper.assertTrue(done(player, "root") && done(player, "pufferfish_trainer") && done(player, "experienced_pufferfish_trainer"),
                    "Material capture did not grant all lower ranks: " + variant.entity().getId());
            boolean elite = variant.fish() == PufferfishItems.HEAVY || variant.fish() == PufferfishItems.RADIANT;
            helper.assertTrue(done(player, "legendary_pufferfish_trainer") == elite, "Incorrect legendary rank: " + variant.entity().getId());
            helper.assertTrue(advancement(player, "legendary_pufferfish_trainer").value().display().orElseThrow().getType() == AdvancementType.CHALLENGE,
                    "Legendary trainer is not a challenge");
            player.getAdvancements().stopListening();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void obtainingBucketsAndCapturingOtherFishDoNotGrantTrainerProgress(GameTestHelper helper) {
        var player = player(helper);
        var buckets = new java.util.ArrayList<ItemStack>();
        buckets.add(new ItemStack(Items.PUFFERFISH_BUCKET));
        for (var color : DyeColor.values()) buckets.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), color));
        PufferfishMobs.VARIANTS.values().forEach(variant -> buckets.add(variant.bucket().toStack()));
        for (var bucket : buckets) {
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), bucket);
        }
        CriteriaTriggers.RECIPE_CRAFTED.trigger(player, PufferfishPlease.id("bucket_pufferfish"),
                List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(Items.WATER_BUCKET)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        var cod = EntityType.COD.create(helper.getLevel());
        cod.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(cod.isRemoved() && player.getMainHandItem().is(Items.COD_BUCKET), "Cod was not captured");
        helper.assertTrue(!done(player, "root") && !done(player, "pufferfish_trainer")
                        && !done(player, "experienced_pufferfish_trainer") && !done(player, "legendary_pufferfish_trainer"),
                "Obtaining a bucket, crafting one, or capturing another fish granted trainer progress");
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void collectionEquipmentEffectsAndBoatsUnlockTheirAdvancements(GameTestHelper helper) {
        var player = player(helper);
        var level = helper.getLevel();
        for (String id : List.of("root", "careful_with_spikes", "wax_on", "ocean_visitor", "unfriendly_fish",
                "pufferfish_voyage", "pufferfish_blessing", "pufferfish_curse", "restored_glory", "radiant_promise")) {
            helper.assertTrue(advancement(player, id) != null && advancement(player, id).value().display().isPresent(), "Missing visible advancement: " + id);
        }
        helper.assertTrue(advancement(player, "radiant_promise").value().display().orElseThrow().getType() == AdvancementType.CHALLENGE,
                "Radiant advancement is not a challenge");
        var ordinary = new ItemStack(Items.PUFFERFISH);
        player.getInventory().add(ordinary.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), ordinary);
        helper.assertTrue(done(player, "root"), "Obtaining a pufferfish did not unlock the root");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        CriteriaTriggers.TICK.trigger(player);
        helper.assertTrue(!done(player, "careful_with_spikes"), "Ordinary armor granted the pufferfish headgear advancement");
        player.setItemSlot(EquipmentSlot.HEAD, PufferfishItems.COPPER.toStack());
        CriteriaTriggers.TICK.trigger(player);
        helper.assertTrue(done(player, "careful_with_spikes"), "Wearing a material pufferfish did not grant progress");
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_CURSE, 260));
        helper.assertTrue(done(player, "pufferfish_blessing") && done(player, "pufferfish_curse"), "Effects did not grant their advancements");
        for (var entry : java.util.Map.of(
                "radiant_promise", "radiant_pufferfish",
                "heavy_pufferfish", "heavy_pufferfish",
                "upgrade_template", "pufferfish_upgrade_smithing_template",
                "pufferfish_conduit", "pufferfish_conduit",
                "pufferfish_pump", "pufferfish_pump",
                "skin_craft", "pufferfish_skin").entrySet()) {
            helper.assertTrue(!done(player, entry.getKey()), "Unrelated actions granted progress: " + entry.getKey());
            var item = BuiltInRegistries.ITEM.get(PufferfishPlease.id(entry.getValue()));
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(item));
            helper.assertTrue(done(player, entry.getKey()), "Obtaining an item did not grant progress: " + entry.getKey());
        }
        boolean cooking = ModList.get().isLoaded("kaleidoscope_cookery");
        boolean sashimi = cooking || ModList.get().isLoaded("farmersdelight");
        helper.assertTrue((advancement(player, "pufferfish_sashimi") != null) == sashimi, "Sashimi progress has incorrect mod conditions");
        if (sashimi) {
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), PufferfishItems.SASHIMI.toStack());
            helper.assertTrue(done(player, "pufferfish_sashimi"), "Sashimi did not grant its advancement");
        }
        boolean aspic = cooking && ModList.get().isLoaded("kaleidoscope_chinesefood");
        helper.assertTrue((advancement(player, "fish_aspic") != null) == aspic, "Aspic progress has incorrect mod conditions");
        if (aspic) {
            var item = BuiltInRegistries.ITEM.get(PufferfishPlease.id("pufferfish_aspic"));
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(item));
            helper.assertTrue(done(player, "fish_aspic"), "Aspic did not grant its advancement");
        }
        var vanillaBoat = EntityType.BOAT.create(level);
        player.startRiding(vanillaBoat, true);
        CriteriaTriggers.START_RIDING_TRIGGER.trigger(player);
        helper.assertTrue(!done(player, "pufferfish_voyage"), "Vanilla boat granted the pufferfish voyage");
        helper.assertTrue(!done(player, "radiant_voyage") && !done(player, "heavy_voyage"), "Vanilla boat granted a special voyage");
        player.stopRiding();
        for (var variant : PufferfishBoats.VARIANTS.values()) {
            var boat = variant.entity().get().create(level);
            player.startRiding(boat, true);
            CriteriaTriggers.START_RIDING_TRIGGER.trigger(player);
            helper.assertTrue(done(player, "pufferfish_voyage"), "Missing boat advancement variant: " + variant.entity().getId());
            helper.assertTrue(done(player, "radiant_voyage") == boat.skinBoatName().equals("radiant_pufferfish_skin_boat"),
                    "Incorrect radiant voyage progress: " + boat.skinBoatName());
            helper.assertTrue(done(player, "heavy_voyage") == boat.isHeavy(), "Incorrect heavy voyage progress: " + boat.skinBoatName());
            player.stopRiding();
            player.getAdvancements().revoke(advancement(player, "pufferfish_voyage"), "ride");
            player.getAdvancements().revoke(advancement(player, "radiant_voyage"), "ride");
            player.getAdvancements().revoke(advancement(player, "heavy_voyage"), "ride");
        }
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waxingRequiresAnActionAndSupportsCraftingBlocksAndLivingFish(GameTestHelper helper) {
        var player = player(helper);
        var level = helper.getLevel();
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), PufferfishItems.COPPER_VARIANTS.get(4).toStack());
        helper.assertTrue(!done(player, "wax_on"), "Merely obtaining a waxed fish granted waxing progress");
        CriteriaTriggers.RECIPE_CRAFTED.trigger(player, PufferfishPlease.id("wax_copper_pufferfish"),
                List.of(PufferfishItems.COPPER.toStack(), new ItemStack(Items.HONEYCOMB)));
        helper.assertTrue(done(player, "wax_on"), "Wax crafting did not grant progress");
        player.getAdvancements().revoke(advancement(player, "wax_on"), "craft");
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, PufferfishBlocks.PLACED_FISH.get().defaultBlockState());
        ((PlacedPufferfishBlockEntity) level.getBlockEntity(pos)).setFish(PufferfishItems.COPPER.toStack());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
        CopperPufferfish.interact(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(pos.getCenter(), Direction.UP, pos, false)));
        helper.assertTrue(done(player, "wax_on"), "Waxing a placed fish did not grant progress");
        player.getAdvancements().revoke(advancement(player, "wax_on"), "wax");
        var fish = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
        fish.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(done(player, "wax_on"), "Waxing a living fish did not grant progress");
        level.removeBlock(pos, false);
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void discoveryRequiresEnteringTheMatchingRuin(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        for (boolean ancient : new boolean[] {false, true}) {
            var floor = helper.absolutePos(new BlockPos(2, ancient ? 50 : 30, 2));
            String id = ancient ? "unfriendly_fish" : "ocean_visitor";
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .get(PufferfishPlease.id(ancient ? "ancient_pufferfish_ruin" : "pufferfish_ruin"));
            var chunk = level.getChunkAt(floor);
            var start = new StructureStart(structure, new ChunkPos(floor), 0,
                    new PiecesContainer(List.of(new PufferfishRuinPiece(floor, ancient, 74392L))));
            level.structureManager().setStartForStructure(SectionPos.of(floor), structure, start, chunk);
            level.structureManager().addReferenceForStructure(SectionPos.of(floor), structure, new ChunkPos(floor).toLong(), chunk);
            player.setPos(floor.above(12).getCenter());
            CriteriaTriggers.LOCATION.trigger(player);
            helper.assertTrue(!done(player, id), "Standing outside a ruin granted discovery");
            player.setPos(floor.above(4).getCenter());
            CriteriaTriggers.LOCATION.trigger(player);
            helper.assertTrue(done(player, id), "Entering a ruin did not grant discovery");
            level.structureManager().setStartForStructure(SectionPos.of(floor), structure, StructureStart.INVALID_START, chunk);
        }
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void restorationRequiresPersonallyCompletingAnUncanceledFrame(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        var observer = player(helper);
        var center = helper.absolutePos(new BlockPos(2, 100, 2));
        for (var offset : BlockPos.betweenClosed(-2, -2, -2, 2, 2, 2)) {
            int x = Math.abs(offset.getX()), y = Math.abs(offset.getY()), z = Math.abs(offset.getZ());
            boolean frame = x == 0 && (y == 2 || z == 2) || y == 0 && (x == 2 || z == 2) || z == 0 && (x == 2 || y == 2);
            level.setBlock(center.offset(offset), (frame ? Blocks.PRISMARINE : Blocks.WATER).defaultBlockState(), 2);
        }
        level.setBlockAndUpdate(center, PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState());
        var last = center.east(2);
        var previous = BlockSnapshot.create(level.dimension(), level, last);
        level.setBlockAndUpdate(last, Blocks.PRISMARINE_BRICKS.defaultBlockState());
        NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(previous, Blocks.PRISMARINE.defaultBlockState(), player));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!done(player, "restored_glory") && !done(observer, "restored_glory"),
                    "An already complete frame or a nearby observer earned restoration");
            level.setBlockAndUpdate(last, Blocks.WATER.defaultBlockState());
            var snapshot = BlockSnapshot.create(level.dimension(), level, last);
            level.setBlockAndUpdate(last, Blocks.PRISMARINE.defaultBlockState());
            var canceled = new BlockEvent.EntityPlaceEvent(snapshot, Blocks.PRISMARINE.defaultBlockState(), player);
            NeoForge.EVENT_BUS.post(canceled);
            canceled.setCanceled(true);
            level.setBlockAndUpdate(last, Blocks.WATER.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!done(player, "restored_glory"), "Canceled placement granted restoration");
                var valid = BlockSnapshot.create(level.dimension(), level, last);
                level.setBlockAndUpdate(last, PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
                NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(valid, Blocks.PRISMARINE.defaultBlockState(), player));
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(done(player, "restored_glory") && !done(observer, "restored_glory"),
                            "Restoration did not credit only the player who completed the frame");
                    for (var pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) level.removeBlock(pos, false);
                    player.getAdvancements().stopListening();
                    observer.getAdvancements().stopListening();
                    helper.succeed();
                });
            });
        });
    }

    private static AdvancementHolder advancement(ServerPlayer player, String id) {
        return player.server.getAdvancements().get(PufferfishPlease.id("adventure/" + id));
    }

    private static boolean done(ServerPlayer player, String id) {
        var advancement = advancement(player, id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        TestPlayers.allowDamage(player);
        player.setPos(helper.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        return player;
    }
}
