// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class CopperPufferfishGameTests {
    @GameTest(template = "empty")
    public static void copperSkinsPreserveStagesAndWaxInCrafting(GameTestHelper helper) {
        for (int i = 0; i < 8; i++) {
            var skin = PufferfishBlocks.COPPER_SKINS.get(i).get();
            var fish = new ItemStack(PufferfishItems.COPPER_VARIANTS.get(i).get());
            helper.assertTrue(PufferfishBlocks.skinForFish(fish) == skin, "Processing lost the copper stage");
            helper.assertTrue(skin.defaultBlockState().isRandomlyTicking() == (i < 3), "Incorrect skin random ticking");
            var stack = new ItemStack(skin);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Skin test"));
            var input = CraftingInput.of(2, 1, List.of(stack, new ItemStack(Items.HONEYCOMB)));
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent() == (i < 4), "Unexpected skin waxing recipe");
            if (i < 4) {
                var result = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(CopperPufferfish.skinIndex(result) == i + 4
                        && result.get(DataComponents.CUSTOM_NAME).equals(stack.get(DataComponents.CUSTOM_NAME)), "Waxing lost skin data");
            }
            var drops = net.minecraft.world.level.block.Block.getDrops(skin.defaultBlockState(), helper.getLevel(),
                    helper.absolutePos(new BlockPos(1, 2, 1)), null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(skin.asItem()), "Skin dropped a different copper stage");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperSkinWeatheringPreservesAttachment(GameTestHelper helper) {
        var relative = new BlockPos(1, 2, 1);
        var pos = helper.absolutePos(relative);
        for (var direction : Direction.values()) helper.setBlock(relative.relative(direction), net.minecraft.world.level.block.Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var random = RandomSource.create(42);
        for (var face : net.minecraft.world.level.block.state.properties.AttachFace.values()) {
            for (var facing : Direction.Plane.HORIZONTAL) {
                var initial = PufferfishBlocks.COPPER_SKIN.get().defaultBlockState()
                        .setValue(PufferfishSkinBlock.FACE, face).setValue(PufferfishSkinBlock.FACING, facing);
                helper.getLevel().setBlockAndUpdate(pos, initial);
                for (int i = 0; i < 10000; i++) CopperPufferfish.randomTick(helper.getLevel(), pos, random);
                helper.assertTrue(helper.getLevel().getBlockState(pos).is(PufferfishBlocks.COPPER_SKINS.get(3).get()), "Skin did not oxidize");
                for (int stage = 3; stage >= 0; stage--) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 2));
                    use(player, pos);
                    for (int i = 0; i < 100; i++) CopperPufferfish.randomTick(helper.getLevel(), pos, random);
                    var state = helper.getLevel().getBlockState(pos);
                    helper.assertTrue(state.is(PufferfishBlocks.COPPER_SKINS.get(stage + 4).get())
                            && player.getMainHandItem().getCount() == 1, "Wax did not protect the skin");
                    helper.assertTrue(state.getValue(PufferfishSkinBlock.FACE) == face
                            && state.getValue(PufferfishSkinBlock.FACING) == facing, "Skin orientation changed");
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                    use(player, pos);
                    helper.assertTrue(helper.getLevel().getBlockState(pos).is(PufferfishBlocks.COPPER_SKINS.get(stage).get()), "Skin wax removal failed");
                    use(player, pos);
                    helper.assertTrue(helper.getLevel().getBlockState(pos).is(PufferfishBlocks.COPPER_SKINS.get(Math.max(0, stage - 1)).get()), "Skin scraping failed");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oxidationStrengthensAttackAndRetaliationPoison(GameTestHelper helper) {
        for (int stage = 0; stage < 8; stage++) {
            var fish = new ItemStack(PufferfishItems.COPPER_VARIANTS.get(stage).get());
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var target = EntityType.PIG.create(helper.getLevel());
            player.setItemSlot(EquipmentSlot.MAINHAND, fish);
            player.getRandom().setSeed(4096);
            helper.assertTrue(target.hurt(target.damageSources().playerAttack(player), 1), "Melee damage failed");
            assertPoison(helper, target, stage);

            PufferfishEffectsGameTests.clearPoison(target);
            PufferfishCombat.hitEffects(fish, target, player, Vec3.ZERO);
            assertPoison(helper, target, stage);

            PufferfishEffectsGameTests.clearPoison(target);
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.HEAD, fish);
            target.invulnerableTime = 0;
            helper.assertTrue(target.hurt(target.damageSources().thorns(player), 1), "Thorns damage failed");
            var poison = PufferfishEffectsGameTests.poison(target);
            helper.assertTrue(stage >= 4 ? poison == null
                    : poison != null && poison.getAmplifier() == new int[] {0, 0, 1, 1}[stage] && poison.getDuration() == 120,
                    "Copper headgear did not apply its oxidation poison tier and duration");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waxDoesNotRemoveExistingPoisonOrThorns(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var target = EntityType.PIG.create(helper.getLevel());
        var fish = new ItemStack(PufferfishItems.COPPER_VARIANTS.get(7).get());
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
        PufferfishCombat.hitEffects(fish, target, player, Vec3.ZERO);
        var poison = PufferfishEffectsGameTests.poison(target);
        helper.assertTrue(poison != null && poison.getDuration() == 200 && poison.getAmplifier() == 2,
                "Wax changed an existing poison effect");
        PufferfishEffectsGameTests.clearPoison(target);
        player.setItemSlot(EquipmentSlot.HEAD, fish);
        player.getRandom().setSeed(4291);
        int reflected = 0;
        for (int i = 0; i < 64; i++) {
            player.invulnerableTime = 0;
            player.setHealth(player.getMaxHealth());
            target.invulnerableTime = 0;
            target.setHealth(target.getMaxHealth());
            player.hurt(player.damageSources().mobAttack(target), 1);
            if (target.getHealth() < target.getMaxHealth()) reflected++;
            helper.assertTrue(!PufferfishEffectsGameTests.hasPoison(target), "Waxed headgear poisoned its attacker");
        }
        helper.assertTrue(reflected > 0, "Wax disabled thorns retaliation");
        helper.succeed();
    }

    private static void assertPoison(GameTestHelper helper, LivingEntity target, int stage) {
        var poison = PufferfishEffectsGameTests.poison(target);
        if (stage >= 4) {
            helper.assertTrue(poison == null, "Waxed copper applied poison");
        } else {
            int duration = 120;
            int amplifier = new int[] {0, 0, 1, 1}[stage];
            helper.assertTrue(poison != null && poison.getDuration() == duration
                    && poison.getAmplifier() == amplifier, "Incorrect oxidation poison strength");
        }
    }

    @GameTest(template = "empty")
    public static void waxingPreservesComponentsAndRejectsWaxedFish(GameTestHelper helper) {
        for (int i = 0; i < 8; i++) {
            var fish = new ItemStack(PufferfishItems.COPPER_VARIANTS.get(i).get(), 8);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Copper test"));
            var input = CraftingInput.of(2, 1, List.of(fish, new ItemStack(Items.HONEYCOMB)));
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent() == (i < 4), "Unexpected waxing recipe match");
            if (i < 4) {
                var result = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(CopperPufferfish.index(result) == i + 4 && result.getCount() == 1
                        && result.get(DataComponents.CUSTOM_NAME).equals(fish.get(DataComponents.CUSTOM_NAME)), "Waxing lost fish data");
            }
            helper.assertTrue(PufferfishItems.canThrow(fish)
                    && PufferfishCombat.damage(fish) == PufferfishCombat.damage(new ItemStack(PufferfishItems.COPPER.get())),
                    "Copper variants have inconsistent combat properties");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedFishOxidizeAndAxesRemoveWaxBeforeRust(GameTestHelper helper) {
        var relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, PufferfishBlocks.PLACED_FISH.get());
        var pos = helper.absolutePos(relative);
        var placed = (PlacedPufferfishBlockEntity) helper.getLevel().getBlockEntity(pos);
        placed.setFish(new ItemStack(PufferfishItems.COPPER.get()));
        var random = RandomSource.create(42);
        for (int i = 0; i < 10000; i++) CopperPufferfish.randomTick(helper.getLevel(), pos, random);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 3, "Copper fish did not fully oxidize");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 2));
        use(player, pos);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 7 && player.getMainHandItem().getCount() == 1, "Waxing failed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        use(player, pos);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 3, "Axe removed oxidation before wax");
        use(player, pos);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 2 && player.getMainHandItem().getDamageValue() == 2, "Scraping failed");
        for (int stage = 0; stage < 4; stage++) {
            placed.setFish(new ItemStack(PufferfishItems.COPPER_VARIANTS.get(stage + 4).get()));
            for (int i = 0; i < 10000; i++) CopperPufferfish.randomTick(helper.getLevel(), pos, random);
            helper.assertTrue(CopperPufferfish.index(placed.fish()) == stage + 4, "Waxed fish oxidized");
        }
        helper.succeed();
    }

    private static void use(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
}
