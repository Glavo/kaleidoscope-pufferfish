// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class NetheritePufferfishGameTests {
    private static ItemStack netherite() {
        return new ItemStack(PufferfishItems.NETHERITE.get());
    }

    @GameTest(template = "empty")
    public static void everyColorUpgradesToTheSameItem(GameTestHelper helper) {
        List<DyeColor> colors = new ArrayList<>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        var manager = helper.getLevel().getRecipeManager();
        for (var color : colors) {
            var base = new ItemStack(Items.PUFFERFISH, 5);
            if (color != null) PufferfishColors.dye(base, color);
            var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(Items.NETHERITE_INGOT));
            var recipe = manager.getRecipeFor(RecipeType.SMITHING, input, helper.getLevel()).orElseThrow().value();
            var result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.getCount() == 1 && ItemStack.isSameItemSameComponents(result, netherite()), "Upgrade retained a color or changed default attributes");
            helper.assertTrue(base.getCount() == 5 && PufferfishColors.get(base) == color, "Upgrade mutated its input");
            base.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
            result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.getHoverName().getString().equals("Keepsake"), "Upgrade lost the custom name");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                var codec = KaleidoscopePufferfish.NETHERITE_UPGRADE.get().streamCodec();
                codec.encode(buffer, (NetheritePufferfishUpgradeRecipe) recipe);
                var synced = codec.decode(buffer);
                helper.assertTrue(synced.matches(input, helper.getLevel()) && ItemStack.isSameItemSameComponents(
                        result, synced.assemble(input, helper.getLevel().registryAccess())), "Synced smithing recipe changed its output");
            } finally {
                buffer.release();
            }
        }
        for (var input : List.of(
                new SmithingRecipeInput(ItemStack.EMPTY, new ItemStack(Items.PUFFERFISH), new ItemStack(Items.NETHERITE_INGOT)),
                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(Items.NETHERITE_INGOT)),
                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.PUFFERFISH), new ItemStack(Items.IRON_INGOT)),
                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), netherite(), new ItemStack(Items.NETHERITE_INGOT)))) {
            helper.assertTrue(manager.getRecipeFor(RecipeType.SMITHING, input, helper.getLevel()).isEmpty(), "Invalid upgrade ingredients were accepted");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smithingMenuConsumesOneOfEachIngredient(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new SmithingMenu(1, player.getInventory());
        var base = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 2), DyeColor.BLUE);
        var ingredients = List.of(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2), base, new ItemStack(Items.NETHERITE_INGOT, 2));
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(menu.getSlot(i).mayPlace(ingredients.get(i)), "Smithing input slot rejected the ingredient");
            menu.getSlot(i).set(ingredients.get(i));
        }
        menu.createResult();
        var slot = menu.getSlot(3);
        helper.assertTrue(slot.mayPickup(player) && PufferfishItems.isNetherite(slot.getItem()), "Smithing menu did not expose the upgrade");
        var taken = slot.remove(1);
        slot.onTake(player, taken);
        helper.assertTrue(taken.getCount() == 1, "Smithing produced the wrong count");
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(menu.getSlot(i).getItem().getCount() == 1, "Smithing consumed the wrong ingredient count");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteBlocksPackUnpackAndDropCorrectly(GameTestHelper helper) {
        var level = helper.getLevel();
        var input = CraftingInput.of(3, 3, Collections.nCopies(9, netherite()));
        var compressed = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value().assemble(input, level.registryAccess());
        helper.assertTrue(compressed.is(PufferfishBlocks.NETHERITE.get().asItem()) && compressed.getCount() == 1, "Netherite compression failed");
        var unpackInput = CraftingInput.of(1, 1, List.of(compressed));
        var unpacked = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, unpackInput, level).orElseThrow().value().assemble(unpackInput, level.registryAccess());
        helper.assertTrue(unpacked.getCount() == 9 && ItemStack.isSameItemSameComponents(unpacked, netherite()), "Unpacking lost netherite attributes");
        var state = PufferfishBlocks.NETHERITE.get().defaultBlockState();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, state);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!player.hasCorrectToolForDrops(state), "Netherite block did not require a tool");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(player.hasCorrectToolForDrops(state), "Diamond pickaxe could not harvest the block");
        var drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        helper.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.getFirst(), compressed), "Block used the wrong loot table");
        helper.assertTrue(netherite().has(DataComponents.FIRE_RESISTANT) && compressed.has(DataComponents.FIRE_RESISTANT), "Netherite items were not fire resistant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteHeadgearAddsAndRemovesArmor(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.inventoryMenu.getSlot(9).set(netherite().copyWithCount(3));
        player.inventoryMenu.quickMoveStack(player, 9);
        player.tick();
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).getCount() == 1
                && player.inventoryMenu.getSlot(9).getItem().getCount() == 2, "Netherite quick equip did not move one item");
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 3
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 3, "Netherite headgear did not grant 3 armor and 3 toughness");
        var breathe = new LivingBreatheEvent(player, false, 1, 0);
        NeoForge.EVENT_BUS.post(breathe);
        helper.assertTrue(!breathe.canBreathe(), "Netherite headgear granted unlimited underwater breathing");
        var fish = EntityType.PUFFERFISH.create(helper.getLevel());
        helper.assertTrue(!player.hurt(player.damageSources().mobAttack(fish), 3), "Netherite headgear lost sting immunity");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, netherite());
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0, "Armor remained after removing the headgear");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteRetaliationUsesStrongerThornsAndPoison(GameTestHelper helper) {
        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = EntityType.PIG.create(helper.getLevel());
        int[] reflected = new int[2];
        for (int tier = 0; tier < 2; tier++) {
            wearer.setItemSlot(EquipmentSlot.HEAD, tier == 0 ? new ItemStack(Items.PUFFERFISH) : netherite());
            helper.getLevel().getRandom().setSeed(92183);
            attacker.getRandom().setSeed(78194);
            for (int i = 0; i < 256; i++) {
                wearer.setHealth(wearer.getMaxHealth());
                wearer.invulnerableTime = 0;
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                attacker.removeEffect(MobEffects.POISON);
                wearer.hurt(wearer.damageSources().mobAttack(attacker), 1);
                var poison = attacker.getEffect(MobEffects.POISON);
                helper.assertTrue(poison != null && poison.getDuration() == (tier == 0 ? 60 : 120)
                        && poison.getAmplifier() == tier, "Retaliation used the wrong poison tier");
                if (attacker.getHealth() < attacker.getMaxHealth()) reflected[tier]++;
            }
        }
        helper.assertTrue(reflected[1] > reflected[0] && reflected[1] < 256, "Netherite headgear did not increase the thorns proc rate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteThrowsDealSixDamageAndRecover(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        var stack = netherite().copyWithCount(3);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
        var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
        helper.assertTrue(stack.getCount() == 2 && projectiles.size() == 1, "Netherite throw failed to consume and spawn one fish");
        var projectile = projectiles.getFirst();
        var saved = new CompoundTag();
        projectile.saveWithoutId(saved);
        var restored = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
        restored.load(saved);
        var receiver = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
        receiver.getEntityData().assignValues(projectile.getEntityData().getNonDefaultValues());
        helper.assertTrue(ItemStack.isSameItemSameComponents(restored.getItem(), stack)
                && ItemStack.isSameItemSameComponents(receiver.getItem(), stack), "Netherite projectile identity was lost on save or sync");
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        target.setNoAi(true);
        projectile.setPos(target.getX() - 1, target.getY() + 0.5, target.getZ());
        projectile.setDeltaMovement(2, 0, 0);
        float health = target.getHealth();
        projectile.tick();
        helper.assertTrue(target.getHealth() == health - 6, "Netherite projectile did not deal exactly 6 base damage");
        var poison = target.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null && poison.getDuration() == 120 && poison.getAmplifier() == 1, "Netherite projectile did not apply six seconds of Poison II");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3));
        helper.assertTrue(projectile.isRemoved() && drops.size() == 1 && drops.getFirst().getItem().getCount() == 1
                && ItemStack.isSameItemSameComponents(drops.getFirst().getItem(), stack), "Recovered projectile lost its netherite identity or name");
        drops.forEach(ItemEntity::discard);
        target.discard();
        helper.succeed();
    }
}
