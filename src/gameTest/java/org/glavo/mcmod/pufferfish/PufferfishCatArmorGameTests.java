// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCatArmorGameTests {
    @GameTest(template = "empty")
    public static void everySkinCraftsEquipsRepairsAndPersists(GameTestHelper helper) {
        var player = player(helper);
        var level = helper.getLevel();
        for (var variant : PufferfishVariant.ALL) {
            String name = variant.skinName() + "_cat_armor";
            var item = PufferfishCatArmor.VARIANTS.get(name).get();
            var skin = new ItemStack(variant.skin());
            var input = CraftingInput.of(3, 3, List.of(skin, ItemStack.EMPTY, skin, skin, skin, skin, ItemStack.EMPTY, skin, ItemStack.EMPTY));
            var recipe = level.getRecipeManager().byKey(PufferfishPlease.id(name)).orElseThrow();
            helper.assertTrue(recipe.value().getType() == RecipeType.CRAFTING, "Missing cat armor recipe");
            var crafting = (net.minecraft.world.item.crafting.CraftingRecipe) recipe.value();
            helper.assertTrue(crafting.matches(input, level) && crafting.assemble(input, level.registryAccess()).is(item), "Incorrect cat armor recipe: " + name);
            var cat = cat(helper, player);
            var stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(interact(player, cat).isCanceled() && player.getMainHandItem().isEmpty(), "Equipping did not consume one armor");
            cat.tick();
            var armor = cat.getBodyArmorItem();
            helper.assertTrue(armor.is(item) && cat.canUseSlot(EquipmentSlot.BODY) && cat.getArmorValue() > 0, "Cat armor did not provide protection");
            helper.assertTrue(Math.abs(cat.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.3) < 0.0001, "Cat armor changed movement speed");
            var advancement = level.getServer().getAdvancements().get(PufferfishPlease.id("adventure/cat_armor"));
            helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "Equipping did not grant the cat armor advancement");
            armor.setDamageValue(40);
            player.setItemInHand(InteractionHand.MAIN_HAND, skin.copyWithCount(2));
            helper.assertTrue(interact(player, cat).isCanceled() && player.getMainHandItem().getCount() == 1
                    && armor.getDamageValue() == Math.max(0, 40 - (armor.getMaxDamage() + 3) / 4), "Skin repair failed");
            var data = new CompoundTag();
            cat.saveWithoutId(data);
            var restored = EntityType.CAT.create(level);
            restored.load(data);
            helper.assertTrue(ItemStack.matches(armor, restored.getBodyArmorItem()) && restored.isOwnedBy(player), "Saving lost cat armor or ownership");
            var expected = armor.copy();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            helper.assertTrue(interact(player, cat).isCanceled() && cat.getBodyArmorItem().isEmpty(), "Shears did not remove cat armor");
            helper.assertTrue(player.getInventory().items.stream().anyMatch(stored -> ItemStack.matches(stored, expected)), "Removing armor lost damage or components");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Shears did not lose durability");
            cat.discard();
            restored.discard();
        }
        player.getAdvancements().stopListening();
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equipmentChangesRequireOwnershipAndMatchingRepairMaterials(GameTestHelper helper) {
        var owner = player(helper);
        var stranger = player(helper);
        var cat = cat(helper, owner);
        var armor = PufferfishCatArmor.VARIANTS.get("diamond_pufferfish_skin_cat_armor").toStack();
        stranger.setItemInHand(InteractionHand.MAIN_HAND, armor.copy());
        interact(stranger, cat);
        helper.assertTrue(cat.getBodyArmorItem().isEmpty() && !stranger.getMainHandItem().isEmpty(), "Stranger equipped armor");
        owner.setItemInHand(InteractionHand.MAIN_HAND, armor.copy());
        interact(owner, cat);
        cat.getBodyArmorItem().setDamageValue(40);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        interact(stranger, cat);
        helper.assertTrue(!cat.getBodyArmorItem().isEmpty(), "Stranger removed armor");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishBlocks.IRON_SKIN.get()));
        interact(owner, cat);
        helper.assertTrue(cat.getBodyArmorItem().getDamageValue() == 40 && owner.getMainHandItem().getCount() == 1, "Wrong skin repaired armor");
        var wild = EntityType.CAT.create(helper.getLevel());
        owner.setItemInHand(InteractionHand.MAIN_HAND, armor.copy());
        interact(owner, wild);
        helper.assertTrue(wild.getBodyArmorItem().isEmpty(), "Wild cat accepted armor");
        owner.setGameMode(GameType.CREATIVE);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishBlocks.DIAMOND_SKIN.get()));
        interact(owner, cat);
        helper.assertTrue(owner.getMainHandItem().getCount() == 1 && cat.getBodyArmorItem().getDamageValue() < 40, "Creative repair consumed material");
        owner.getAdvancements().stopListening();
        stranger.getAdvancements().stopListening();
        owner.discard();
        stranger.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorProtectsAndRetaliatesWithTheMatchingMaterialEffect(GameTestHelper helper) {
        var owner = player(helper);
        for (var variant : PufferfishVariant.ALL) {
            var cat = cat(helper, owner);
            cat.setBodyArmorItem(PufferfishCatArmor.VARIANTS.get(variant.skinName() + "_cat_armor").toStack());
            cat.tick();
            var attacker = EntityType.COW.create(helper.getLevel());
            float health = cat.getHealth();
            cat.hurt(cat.damageSources().mobAttack(attacker), 2);
            helper.assertTrue(cat.getHealth() > health - 2 && cat.getHealth() < health, "Cat armor did not reduce damage");
            helper.assertTrue(cat.getBodyArmorItem().getDamageValue() == 1 && attacker.getHealth() < attacker.getMaxHealth(), "Armor did not wear or retaliate");
            if (variant == PufferfishVariant.RADIANT) {
                helper.assertTrue(attacker.getEffect(MobEffects.WITHER).getAmplifier() == 1
                        && attacker.getEffect(MobEffects.WITHER).getDuration() == PufferfishCombat.STATUS_EFFECT_DURATION_TICKS
                        && attacker.getEffect(MobEffects.GLOWING).getDuration() == 200, "Radiant armor did not apply Wither II and glow");
            } else if (variant.waxed()) {
                helper.assertTrue(attacker.getActiveEffects().isEmpty(), "Waxed armor applied an effect");
            } else {
                helper.assertTrue(PufferfishEffectsGameTests.hasPoison(attacker), "Cat armor omitted its poison effect");
            }
            cat.discard();
            attacker.discard();
        }
        owner.getAdvancements().stopListening();
        owner.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorDoesNotRetaliateAgainstOwnersPetsOrProjectilesAndDropsOnDeath(GameTestHelper helper) {
        var owner = player(helper);
        var cat = cat(helper, owner);
        cat.setBodyArmorItem(PufferfishCatArmor.VARIANTS.get("radiant_pufferfish_skin_cat_armor").toStack());
        cat.tick();
        cat.hurt(cat.damageSources().playerAttack(owner), 1);
        helper.assertTrue(owner.getHealth() == owner.getMaxHealth() && owner.getActiveEffects().isEmpty(), "Armor attacked its owner");
        var pet = EntityType.WOLF.create(helper.getLevel());
        pet.setTame(true, true);
        pet.setOwnerUUID(owner.getUUID());
        cat.invulnerableTime = 0;
        cat.hurt(cat.damageSources().mobAttack(pet), 1);
        helper.assertTrue(pet.getHealth() == pet.getMaxHealth() && pet.getActiveEffects().isEmpty(), "Armor attacked a friendly pet");
        var attacker = EntityType.SKELETON.create(helper.getLevel());
        var arrow = EntityType.ARROW.create(helper.getLevel());
        cat.invulnerableTime = 0;
        cat.hurt(cat.damageSources().arrow(arrow, attacker), 1);
        helper.assertTrue(attacker.getHealth() == attacker.getMaxHealth() && attacker.getActiveEffects().isEmpty(), "Armor retaliated against a projectile");
        var expected = cat.getBodyArmorItem().copy();
        cat.invulnerableTime = 0;
        cat.hurt(cat.damageSources().fellOutOfWorld(), 100);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(2), item -> ItemStack.matches(item.getItem(), expected));
        helper.assertTrue(drops.size() == 1, "Cat death did not drop exactly one equipped armor");
        drops.forEach(ItemEntity::discard);
        owner.getAdvancements().stopListening();
        owner.discard();
        helper.succeed();
    }

    private static PlayerInteractEvent.EntityInteract interact(ServerPlayer player, Cat cat) {
        return NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cat));
    }

    private static Cat cat(GameTestHelper helper, ServerPlayer owner) {
        var cat = EntityType.CAT.create(helper.getLevel());
        cat.setTame(true, true);
        cat.setOwnerUUID(owner.getUUID());
        cat.setNoAi(true);
        cat.setNoGravity(true);
        cat.setPos(helper.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        return cat;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "cat-armor-test"), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        helper.getLevel().addFreshEntity(player);
        return player;
    }
}
