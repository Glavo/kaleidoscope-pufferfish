// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;
import org.glavo.mcmod.pufferfish.test.TestEffects;
import org.glavo.mcmod.pufferfish.test.TestItems;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class NetheritePufferfishGameTests {
    private static ItemStack netherite() {
        return new ItemStack(PufferfishItems.NETHERITE.get());
    }

    @GameTest(template = "empty")
    public static void onlyDiamondFishUpgradeAndPreserveComponents(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        var addition = new ItemStack(Items.NETHERITE_INGOT);
        var base = new ItemStack(PufferfishItems.DIAMOND.get(), 5);
        var input = new SmithingRecipeInput(template, base, addition);
        var recipe = manager.getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
        var result = recipe.assemble(input, level.registryAccess());
        helper.assertTrue(result.getCount() == 1 && ItemStack.isSameItemSameComponents(result, netherite()),
                "Diamond upgrade changed default attributes");
        base.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
        var enchantment = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOYALTY);
        base.enchant(enchantment, 3);
        var original = base.copy();
        result = recipe.assemble(input, level.registryAccess());
        helper.assertTrue(result.getHoverName().getString().equals("Keepsake")
                && result.getEnchantmentLevel(enchantment) == 3, "Upgrade lost the custom name or enchantment");
        helper.assertTrue(ItemStack.matches(base, original), "Upgrade mutated its input");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var codec = PufferfishPlease.NETHERITE_UPGRADE.get().streamCodec();
            codec.encode(buffer, (NetheritePufferfishUpgradeRecipe) recipe);
            var synced = codec.decode(buffer);
            helper.assertTrue(synced.matches(input, level) && ItemStack.isSameItemSameComponents(
                    result, synced.assemble(input, level.registryAccess())), "Synced smithing recipe changed its output");
        } finally {
            buffer.release();
        }
        var invalidBases = new ArrayList<ItemStack>();
        invalidBases.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) {
            invalidBases.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        }
        for (var item : PufferfishItems.COPPER_VARIANTS) invalidBases.add(new ItemStack(item.get()));
        for (var item : List.of(Items.PUFFERFISH_BUCKET, PufferfishItems.IRON.get(), PufferfishItems.GOLDEN.get(),
                PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get())) invalidBases.add(new ItemStack(item));
        for (var invalidBase : invalidBases) {
            helper.assertTrue(!recipe.isBaseIngredient(invalidBase)
                    && manager.getRecipeFor(RecipeType.SMITHING, new SmithingRecipeInput(template, invalidBase, addition), level).isEmpty(),
                    "A non-diamond fish was accepted as an upgrade base");
        }
        for (var invalid : List.of(
                new SmithingRecipeInput(ItemStack.EMPTY, base, addition),
                new SmithingRecipeInput(new ItemStack(Items.PAPER), base, addition),
                new SmithingRecipeInput(template, base, ItemStack.EMPTY),
                new SmithingRecipeInput(template, base, new ItemStack(Items.IRON_INGOT)))) {
            helper.assertTrue(manager.getRecipeFor(RecipeType.SMITHING, invalid, level).isEmpty(), "Invalid upgrade ingredients were accepted");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smithingMenuConsumesOneOfEachIngredient(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new SmithingMenu(1, player.getInventory());
        var base = new ItemStack(PufferfishItems.DIAMOND.get(), 2);
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
        helper.assertTrue(player.hurt(player.damageSources().mobAttack(fish), 3), "Netherite headgear still granted sting immunity");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, netherite());
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0, "Armor remained after removing the headgear");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteRetaliationUsesStrongerThornsWithConditionalPoison(GameTestHelper helper) {
        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = EntityType.PIG.create(helper.getLevel());
        int[] reflected = new int[2];
        for (int tier = 0; tier < 2; tier++) {
            wearer.setItemSlot(EquipmentSlot.HEAD, tier == 0 ? new ItemStack(Items.PUFFERFISH) : netherite());
            helper.getLevel().getRandom().setSeed(92183);
            attacker.getRandom().setSeed(78194);
            for (int i = 0; i < 256; i++) {
                var head = wearer.getItemBySlot(EquipmentSlot.HEAD);
                if (head.isDamageableItem()) head.setDamageValue(0);
                wearer.setHealth(wearer.getMaxHealth());
                wearer.invulnerableTime = 0;
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                TestEffects.clearPoison(attacker);
                wearer.hurt(wearer.damageSources().mobAttack(attacker), 1);
                var poison = TestEffects.poison(attacker);
                helper.assertTrue(attacker.getHealth() < attacker.getMaxHealth()
                        ? poison != null && poison.getDuration() == 120 && poison.getAmplifier() == tier : poison == null,
                        "Retaliation did not condition material poison on successful thorns");
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
        var restored = PufferfishPlease.THROWN_PUFFERFISH.get().create(helper.getLevel());
        restored.load(saved);
        var receiver = PufferfishPlease.THROWN_PUFFERFISH.get().create(helper.getLevel());
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
        var poison = TestEffects.poison(target);
        helper.assertTrue(poison != null && poison.getDuration() == 120 && poison.getAmplifier() == 1, "Netherite projectile did not apply six seconds of Poison II");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3));
        helper.assertTrue(projectile.isRemoved() && drops.size() == 1 && drops.getFirst().getItem().getCount() == 1
                && ItemStack.isSameItemSameComponents(drops.getFirst().getItem(), TestItems.worn(stack)), "Recovered projectile lost its netherite identity or name");
        drops.forEach(ItemEntity::discard);
        target.discard();
        helper.succeed();
    }
}
