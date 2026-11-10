// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishShieldGameTests {
    @GameTest(template = "empty")
    public static void successfulSkinBlocksPoisonAndRollThornsInEitherHand(GameTestHelper helper) {
        int reflected = 0;
        int blocked = 0;
        for (var item : PufferfishShields.VARIANTS.values()) for (var hand : InteractionHand.values()) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            player.setNoGravity(true);
            player.setYRot(0);
            player.getRandom().setSeed(1234);
            var shield = new ItemStack(item.get());
            player.setItemInHand(hand, shield);
            shield.use(helper.getLevel(), player, hand);
            for (int tick = 0; tick < 6; tick++) player.tick();
            var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
            attacker.setNoAi(true);
            for (int attempt = 0; attempt < 64; attempt++) {
                attacker.setHealth(20);
                attacker.invulnerableTime = 0;
                attacker.setDeltaMovement(Vec3.ZERO);
                attacker.removeAllEffects();
                player.invulnerableTime = 0;
                shield.setDamageValue(0);
                player.hurt(player.damageSources().mobAttack(attacker), 6);
                var poison = attacker.getEffect(PufferfishItems.isRadiant(shield) ? net.minecraft.world.effect.MobEffects.WITHER : net.minecraft.world.effect.MobEffects.POISON);
                helper.assertTrue(PufferfishItems.isRadiant(shield) && attacker.getHealth() == 20 ? poison == null
                        : poison != null && poison.getDuration() == (PufferfishItems.isRadiant(shield) ? 100 : 60) && poison.getAmplifier() == 0,
                        "Shield applied an incorrect effect or radiant effects without thorns damage");
                helper.assertTrue(player.getHealth() == 20, "Shield did not block incoming damage");
                float damage = 20 - attacker.getHealth();
                helper.assertTrue(damage == 0 || damage >= 1 && damage <= 5, "Shield exceeded native Thorns I damage");
                if (damage > 0) reflected++;
                blocked++;
            }
            attacker.discard();
        }
        helper.assertTrue(reflected > 0 && reflected < blocked, "Thorns never triggered or triggered on every block");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unsuccessfulBlocksAndOrdinaryShieldsDoNotRetaliate(GameTestHelper helper) {
        for (int scenario = 0; scenario < 5; scenario++) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            player.setNoGravity(true);
            player.setYRot(scenario == 1 ? 180 : 0);
            var shield = new ItemStack(scenario == 2 ? Items.SHIELD : PufferfishShields.VARIANTS.get("pufferfish_skin_shield").get());
            player.setItemInHand(InteractionHand.OFF_HAND, shield);
            if (scenario != 0) shield.use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            for (int tick = 0; tick < 6; tick++) player.tick();
            var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
            attacker.setNoAi(true);
            final int mode = scenario;
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent> intercept = event -> {
                if (event.getEntity() == player) {
                    if (mode == 3) event.setCanceled(true);
                    if (mode == 4) event.setBlockedDamage(0);
                }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, intercept);
            try {
                player.hurt(player.damageSources().mobAttack(attacker), 6);
                helper.assertTrue(attacker.getHealth() == 20 && !attacker.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                        "Invalid shield block retaliated: scenario=" + scenario);
            } finally {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(intercept);
                attacker.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coveringShieldsPreservesComponentsAndMatchesEverySkin(GameTestHelper helper) {
        var level = helper.getLevel();
        var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        for (var entry : PufferfishShields.VARIANTS.entrySet()) {
            var skinId = KaleidoscopePufferfish.id(entry.getKey().replace("_shield", ""));
            var skin = new ItemStack(BuiltInRegistries.ITEM.get(skinId));
            for (boolean reversed : new boolean[] {false, true}) {
                var shield = new ItemStack(Items.SHIELD);
                shield.setDamageValue(87);
                shield.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                EnchantmentHelper.updateEnchantments(shield, values -> values.set(unbreaking, 3));
                var input = CraftingInput.of(2, 2, reversed ? List.of(skin, ItemStack.EMPTY, ItemStack.EMPTY, shield)
                        : List.of(shield, ItemStack.EMPTY, ItemStack.EMPTY, skin));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
                var output = recipe.assemble(input, level.registryAccess());
                helper.assertTrue(output.is(entry.getValue().get()) && output.getCount() == 1 && output.getMaxDamage() == 336
                        && output.getDamageValue() == 87 && output.getHoverName().equals(shield.getHoverName())
                        && output.getEnchantments().getLevel(unbreaking) == 3, "Skin covering lost shield properties or selected the wrong variant");
                helper.assertTrue(shield.getDamageValue() == 87 && skin.getCount() == 1
                        && recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Recipe mutated inputs or duplicated materials");
                helper.assertTrue(unbreaking.value().isSupportedItem(output), "Covered shield lost Unbreaking support");
                boolean metal = entry.getKey().startsWith("netherite_") || entry.getKey().startsWith("radiant_") || entry.getKey().startsWith("heavy_");
                helper.assertTrue(output.has(DataComponents.FIRE_RESISTANT) == metal, "Skin covering lost its material's fire resistance");
            }
            for (var invalid : List.of(
                    CraftingInput.of(2, 1, List.of(skin, new ItemStack(Items.STICK))),
                    CraftingInput.of(3, 1, List.of(new ItemStack(Items.SHIELD), skin, new ItemStack(Items.DIRT))),
                    CraftingInput.of(2, 1, List.of(new ItemStack(entry.getValue().get()), skin)))) {
                helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, invalid, level).isEmpty(),
                        "Skin shield recipe accepted invalid ingredients");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skinShieldsBlockDamageWearOutAndCanBeDisabled(GameTestHelper helper) {
        for (var item : PufferfishShields.VARIANTS.values()) for (var hand : InteractionHand.values()) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            player.setNoGravity(true);
            player.setYRot(0);
            var shield = new ItemStack(item.get());
            player.setItemInHand(hand, shield);
            shield.use(helper.getLevel(), player, hand);
            for (int tick = 0; tick < 6; tick++) player.tick();
            helper.assertTrue(player.isBlocking(), "Skin shield failed to enter blocking state");
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 4));
            attacker.setNoAi(true);
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == player.getMaxHealth() && shield.getDamageValue() >= 7 && shield.getDamageValue() <= 9,
                    "Skin shield failed to block frontal damage or consume durability");
            player.disableShield();
            helper.assertTrue(!player.isBlocking() && player.getCooldowns().isOnCooldown(item.get()),
                    "Shield disable did not affect the skin shield");
            helper.assertTrue(item.get().isValidRepairItem(shield, new ItemStack(Items.OAK_PLANKS)), "Skin shield lost plank repair");
            attacker.discard();
        }
        helper.succeed();
    }
}
