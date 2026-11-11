// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishDurabilityGameTests {
    public static ItemStack worn(ItemStack original) {
        var result = original.copyWithCount(1);
        result.set(DataComponents.MAX_STACK_SIZE, 1);
        result.set(DataComponents.MAX_DAMAGE, PufferfishDurability.maximum(original));
        result.set(DataComponents.DAMAGE, original.getOrDefault(DataComponents.DAMAGE, 0) + 1);
        return result;
    }

    private static ArrayList<ItemStack> variants() {
        var stacks = new ArrayList<ItemStack>();
        stacks.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) stacks.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        PufferfishMobs.VARIANTS.values().forEach(variant -> stacks.add(variant.fish().toStack()));
        PufferfishItems.FLAIL_VARIANTS.forEach(item -> stacks.add(item.toStack()));
        return stacks;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "durability-test"), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND),
                player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        return player;
    }

    @GameTest(template = "empty")
    public static void pristineFishStackWornFishPersistAndRepairRestoresStacking(GameTestHelper helper) {
        for (var original : variants()) {
            original.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keepsake"));
            int capacity = PufferfishItems.isFlail(original) ? 1 : 64;
            helper.assertTrue(original.getMaxStackSize() == capacity && !original.isDamaged(), "Pristine fish lost its original stack limit");
            var stack = original.copy();
            PufferfishDurability.damage(stack, helper.getLevel(), null, null);
            helper.assertTrue(stack.getCount() == 1 && stack.isDamageableItem() && stack.getDamageValue() == 1
                    && stack.getMaxStackSize() == 1 && stack.isBarVisible(), "Used fish did not become a single durable item");
            var restored = ItemStack.parse(helper.getLevel().registryAccess(), stack.save(helper.getLevel().registryAccess())).orElseThrow();
            helper.assertTrue(ItemStack.matches(stack, restored), "Saving lost fish durability or components");
            restored.setDamageValue(0);
            PufferfishDurability.normalize(restored);
            helper.assertTrue(ItemStack.matches(restored, original), "Repair did not restore the original components and stackability");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meleeSeparatesOneFishAndFullInventoriesCancelWithoutLoss(GameTestHelper helper) {
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(Items.PUFFERFISH, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        player.attack(target);
        helper.assertTrue(stack.getCount() == 64 && !stack.isDamaged() && target.getHealth() == target.getMaxHealth(), "Full inventory attack lost or damaged fish");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        player.attack(target);
        helper.assertTrue(stack.getCount() == 1 && stack.getDamageValue() == 1 && player.getInventory().getItem(1).getCount() == 63
                && !player.getInventory().getItem(1).isDamaged(), "Melee did not separate exactly one fish");
        stack.setDamageValue(stack.getMaxDamage() - 1);
        target.invulnerableTime = 0;
        player.attack(target);
        helper.assertTrue(stack.isEmpty() && player.getInventory().countItem(Items.PUFFERFISH) == 63, "Breaking the active fish consumed the reserve stack");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unbreakingReducesWearAndCreativeUseCostsNothing(GameTestHelper helper) {
        var regular = new ItemStack(PufferfishItems.NETHERITE.get());
        var enchanted = regular.copy();
        enchanted.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
        helper.getLevel().getRandom().setSeed(82193);
        for (int hit = 0; hit < 1000; hit++) {
            PufferfishDurability.damage(regular, helper.getLevel(), null, null);
            PufferfishDurability.damage(enchanted, helper.getLevel(), null, null);
        }
        helper.assertTrue(regular.getDamageValue() == 1000 && enchanted.getDamageValue() > 150 && enchanted.getDamageValue() < 350,
                "Unbreaking III did not reduce wear at the native rate");
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        var untouched = new ItemStack(Items.PUFFERFISH);
        PufferfishDurability.damage(untouched, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        helper.assertTrue(ItemStack.matches(untouched, new ItemStack(Items.PUFFERFISH)), "Creative use changed fish durability");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void experienceOrbsRepairFishInBothHandsAndOnHead(GameTestHelper helper) {
        for (var slot : java.util.List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD)) {
            var player = player(helper);
            var fish = worn(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.BLUE));
            fish.setDamageValue(10);
            fish.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
            player.setItemSlot(slot, fish);
            var orb = new ExperienceOrb(helper.getLevel(), player.getX(), player.getY(), player.getZ(), 8);
            orb.playerTouch(player);
            helper.assertTrue(fish.getDamageValue() == 0 && player.totalExperience == 3, "Mending did not repair ten damage and return unused XP");
            player.doTick();
            helper.assertTrue(fish.getMaxStackSize() == 64 && !fish.has(DataComponents.MAX_DAMAGE)
                    && PufferfishColors.get(fish) == DyeColor.BLUE && !fish.getEnchantments().isEmpty(), "Mending lost metadata or left fish unstackable");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void freshFishRepairInAnvilConsumesOneAndPreservesEnchantments(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        var fish = worn(new ItemStack(Items.PUFFERFISH));
        fish.setDamageValue(40);
        fish.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        var menu = new AnvilMenu(1, player.getInventory());
        menu.getSlot(0).set(fish);
        menu.getSlot(1).set(new ItemStack(Items.PUFFERFISH, 4));
        var result = menu.getSlot(2).remove(1);
        helper.assertTrue(!result.isEmpty() && !result.isDamaged() && result.getMaxStackSize() == 64 && !result.getEnchantments().isEmpty(),
                "Fresh fish did not repair an enchanted fish");
        menu.getSlot(2).onTake(player, result);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount() == 3, "Anvil repair consumed the wrong number of fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wornFishSurviveBucketsEntitySavingAndPlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var pristine : variants()) {
            if (!PufferfishItems.isPufferfish(pristine)) continue;
            var original = worn(pristine);
            original.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(original, new ItemStack(Items.WATER_BUCKET)));
            var recipe = new PufferfishBucketRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            var bucket = recipe.assemble(input, level.registryAccess());
            ((net.minecraft.world.item.MobBucketItem) bucket.getItem()).checkExtraContent(null, level, bucket, pos);
            var fishes = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pufferfish.class, new net.minecraft.world.phys.AABB(pos).inflate(1));
            helper.assertTrue(fishes.size() == 1, "Bucket did not release exactly one fish");
            var fish = fishes.getFirst();
            var data = new net.minecraft.nbt.CompoundTag();
            fish.saveWithoutId(data);
            var loaded = (net.minecraft.world.entity.animal.Pufferfish) fish.getType().create(level);
            loaded.load(data);
            var captured = loaded.getBucketItemStack();
            loaded.saveToBucketTag(captured);
            var stored = ItemStack.parseOptional(level.registryAccess(), captured.get(DataComponents.BUCKET_ENTITY_DATA).copyTag().getCompound("Fish"));
            helper.assertTrue(ItemStack.matches(original, stored), "Release, saving or recapture lost durability or enchantments");
            fish.discard();
            loaded.discard();
            var state = PufferfishBlocks.PLACED_FISH.get().defaultBlockState();
            var placed = new PlacedPufferfishBlockEntity(pos, state);
            placed.setFish(original);
            var restored = new PlacedPufferfishBlockEntity(pos, state);
            restored.loadWithComponents(placed.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            helper.assertTrue(ItemStack.matches(original, restored.fish()), "Placement and saving reset fish durability");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialAndSmithingUpgradesPreserveProportionalWear(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = worn(new ItemStack(Items.PUFFERFISH));
        center.setDamageValue(32);
        center.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        for (var material : java.util.List.of(Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND)) {
            var ring = new ArrayList<ItemStack>();
            for (int slot = 0; slot < 9; slot++) ring.add(slot == 4 ? center : new ItemStack(material));
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, ring);
            var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow().value();
            var result = recipe.assemble(input, level.registryAccess());
            helper.assertTrue(result.getDamageValue() == (int) Math.ceil(result.getMaxDamage() / 2.0) && !result.getEnchantments().isEmpty(),
                    "Material upgrade repaired the fish or lost enchantments");
        }
        var netherite = worn(new ItemStack(PufferfishItems.NETHERITE.get()));
        netherite.setDamageValue(1000);
        for (var addition : java.util.List.of(Items.HEAVY_CORE, Items.NETHER_STAR)) {
            var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(PufferfishItems.UPGRADE_TEMPLATE.toStack(), netherite, new ItemStack(addition));
            var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level).orElseThrow().value();
            var result = recipe.assemble(input, level.registryAccess());
            helper.assertTrue(result.getMaxDamage() == PufferfishDurability.maximum(result)
                    && result.getDamageValue() == (int) Math.ceil(1000.0 * result.getMaxDamage() / 2031), "Smithing reset or mis-scaled durability");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void finalProjectileUseDealsDamageWithoutReturningABrokenFish(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 0)).getCenter());
        for (boolean loyal : new boolean[]{false, true}) {
            var stack = worn(new ItemStack(Items.PUFFERFISH));
            stack.setDamageValue(63);
            if (loyal) stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), 3);
            var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 3));
            target.setNoAi(true);
            var projectile = new org.glavo.mcmod.pufferfish.entity.ThrownPufferfish(helper.getLevel(), player);
            projectile.setItem(stack);
            projectile.setPos(target.position().add(0, 0.5, -1));
            projectile.setDeltaMovement(0, 0, 2);
            helper.getLevel().addFreshEntity(projectile);
            projectile.tick();
            helper.assertTrue(target.getHealth() < target.getMaxHealth() && projectile.isRemoved(), "Last use failed to hit or left a broken projectile alive");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    target.getBoundingBox().inflate(3)).isEmpty() && player.getInventory().countItem(Items.PUFFERFISH) == 0,
                    "Broken projectile produced a recovered fish");
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingRepairProducesValidDurabilityComponentsAndRetainsDye(GameTestHelper helper) {
        for (var item : java.util.List.of(Items.PUFFERFISH, PufferfishItems.FLAIL.get())) {
            var first = worn(new ItemStack(item));
            first.setDamageValue(50);
            first.set(PufferfishPlease.COLOR.get(), DyeColor.RED);
            var second = first.copy();
            var recipe = new net.minecraft.world.item.crafting.RepairItemRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(first, second));
            var result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.getDamageValue() == 33 && result.getMaxDamage() == 64 && result.getMaxStackSize() == 1
                    && result.get(PufferfishPlease.COLOR.get()) == DyeColor.RED, "Crafting repair changed wear or dye");
            helper.assertTrue(ItemStack.parse(helper.getLevel().registryAccess(), result.save(helper.getLevel().registryAccess())).isPresent(),
                    "Crafting repair produced an invalid durable stack");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void headgearAndFlailsConsumeDurabilityAndBreak(GameTestHelper helper) {
        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = EntityType.PIG.create(helper.getLevel());
        var headgear = new ItemStack(Items.PUFFERFISH);
        wearer.setItemSlot(EquipmentSlot.HEAD, headgear);
        wearer.hurt(wearer.damageSources().mobAttack(attacker), 1);
        helper.assertTrue(headgear.getDamageValue() >= 1 && headgear.getDamageValue() <= 3, "Headgear did not consume durability on a successful hit");
        headgear.setDamageValue(63);
        wearer.invulnerableTime = 0;
        wearer.hurt(wearer.damageSources().mobAttack(attacker), 1);
        helper.assertTrue(wearer.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Exhausted headgear did not break");
        wearer.setPos(helper.absolutePos(new BlockPos(2, 2, 0)).getCenter());
        var flail = worn(PufferfishItems.FLAIL.toStack());
        flail.setDamageValue(63);
        wearer.setItemInHand(InteractionHand.MAIN_HAND, flail);
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        var projectile = new org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead(helper.getLevel(), wearer, InteractionHand.MAIN_HAND);
        projectile.setPos(target.position().add(0, 0.5, -1));
        projectile.setDeltaMovement(0, 0, 2);
        helper.getLevel().addFreshEntity(projectile);
        projectile.tick();
        helper.assertTrue(target.getHealth() < target.getMaxHealth() && flail.isEmpty() && projectile.isRemoved(),
                "Last flail hit failed to deal damage and break the weapon");
        target.discard();
        projectile.discard();
        helper.succeed();
    }
}
