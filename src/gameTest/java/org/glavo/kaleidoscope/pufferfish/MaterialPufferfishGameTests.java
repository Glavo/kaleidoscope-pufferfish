// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialPufferfishGameTests {
    private MaterialPufferfishGameTests() {}

    private static List<Item> fish() {
        return List.of(PufferfishItems.COPPER.get(), PufferfishItems.IRON.get(), PufferfishItems.DIAMOND.get());
    }

    private static CraftingInput ring(Item material, ItemStack center) {
        var items = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) items.add(i == 4 ? center : new ItemStack(material));
        return CraftingInput.of(3, 3, items);
    }

    @GameTest(template = "empty")
    public static void materialRingsAcceptEveryFishColor(GameTestHelper helper) {
        var materials = List.of(Items.COPPER_INGOT, Items.IRON_INGOT, Items.DIAMOND);
        var manager = helper.getLevel().getRecipeManager();
        for (int i = 0; i < materials.size(); i++) {
            for (int color = -1; color < 16; color++) {
                var center = new ItemStack(Items.PUFFERFISH);
                if (color >= 0) PufferfishColors.dye(center, DyeColor.byId(color));
                var input = ring(materials.get(i), center);
                var recipe = manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
                helper.assertTrue(ItemStack.matches(recipe.assemble(input, helper.getLevel().registryAccess()), new ItemStack(fish().get(i))),
                        "Material ring produced the wrong fish");
                helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Material ring returned an ingredient");
                for (int slot = 0; slot < 9; slot++) {
                    var incomplete = new ArrayList<>(input.items());
                    incomplete.set(slot, ItemStack.EMPTY);
                    helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, incomplete), helper.getLevel()), "Material ring accepted a missing ingredient");
                }
                var mixed = new ArrayList<>(input.items());
                mixed.set(0, new ItemStack(materials.get((i + 1) % 3)));
                helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, mixed), helper.getLevel()), "Recipe accepted a mixed material ring");
            }
            for (var invalid : List.of(Items.COD, Items.PUFFERFISH_BUCKET, PufferfishItems.GOLDEN.get(), PufferfishItems.NETHERITE.get(), fish().get(i))) {
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, ring(materials.get(i), new ItemStack(invalid)), helper.getLevel()).isEmpty(),
                        "Material ring accepted an invalid center");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialFishUseNormalCombatAndHeadgear(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var rawFish = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:foods/raw_pufferfish"));
        for (var item : fish()) {
            var stack = new ItemStack(item);
            helper.assertTrue(player.inventoryMenu.getSlot(5).mayPlace(stack) && !stack.is(rawFish)
                    && !PufferfishItems.hasNetheriteCombat(stack), "Material fish equipment or ingredient classification is incorrect");
            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
            player.tick();
            helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == PufferfishCombat.damage(new ItemStack(Items.PUFFERFISH))
                    && player.getAttributeValue(Attributes.ATTACK_SPEED) == PufferfishCombatConfig.value(PufferfishCombatConfig.NORMAL_ATTACK_SPEED),
                    "Material fish did not use ordinary fish combat attributes");
            player.setItemSlot(EquipmentSlot.HEAD, stack);
            player.removeAllEffects();
            PufferfishHeadgear.breathe(new PlayerTickEvent.Post(player));
            helper.assertTrue(PufferfishHeadgear.isWearing(player) && player.getEffect(MobEffects.WATER_BREATHING).getDuration() == 200,
                    "Material fish headgear did not provide the ordinary breathing reserve");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialThrowsKeepTheirItemAndShareCooldown(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var allFish = new ArrayList<>(fish());
        allFish.addAll(List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get()));
        for (var item : allFish) {
            for (var other : allFish) player.getCooldowns().removeCooldown(other);
            var stack = new ItemStack(item, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(event);
            var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, player.getBoundingBox().inflate(4));
            helper.assertTrue(event.isCanceled() && stack.getCount() == 1 && projectiles.size() == 1
                    && projectiles.getFirst().getItem().is(item) && projectiles.getFirst().getItem().getCount() == 1,
                    "Throwing lost the material fish identity or consumed the wrong quantity");
            for (var other : allFish) helper.assertTrue(player.getCooldowns().isOnCooldown(other), "Switching fish bypassed the throw cooldown");
            projectiles.getFirst().discard();
        }
        helper.succeed();
    }
}
