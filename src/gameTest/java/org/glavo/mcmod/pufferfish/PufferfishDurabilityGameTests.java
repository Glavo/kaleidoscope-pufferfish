// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import static org.glavo.mcmod.pufferfish.test.TestItems.worn;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestItems;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishDurabilityGameTests {
    @GameTest(template = "empty")
    public static void externalDurabilitySurvivesUseRepairAndUpgrades(GameTestHelper helper) {
        var external = new ItemStack(Items.PUFFERFISH);
        external.set(DataComponents.MAX_STACK_SIZE, 1);
        external.set(DataComponents.MAX_DAMAGE, 777);
        external.set(DataComponents.DAMAGE, 0);
        var expected = external.copy();
        PufferfishDurability.damage(external, helper.getLevel(), null, null);
        helper.assertTrue(external.getMaxDamage() == 777 && external.getDamageValue() == 1, "Using a fish replaced external durability");
        external.setDamageValue(0);
        helper.assertTrue(ItemStack.matches(expected, external), "Repair removed external durability");
        var upgraded = PufferfishDurability.transfer(external, external.transmuteCopy(PufferfishItems.NETHERITE.get(), 1));
        helper.assertTrue(upgraded.getMaxDamage() == 777 && upgraded.getMaxStackSize() == 1,
                "Upgrade replaced external durability");

        var modified = worn(new ItemStack(Items.PUFFERFISH));
        modified.set(DataComponents.MAX_DAMAGE, 999);
        PufferfishDurability.activate(modified);
        upgraded = PufferfishDurability.transfer(modified, modified.transmuteCopy(PufferfishItems.NETHERITE.get(), 1));
        helper.assertTrue(upgraded.getMaxDamage() == 999 && upgraded.getMaxStackSize() == 1
                && !upgraded.has(PufferfishPlease.MANAGED_DURABILITY.get()), "Upgrade overwrote a later external durability change");
        helper.succeed();
    }


    private static ArrayList<ItemStack> variants() {
        var stacks = new ArrayList<>(TestItems.fishVariants());
        PufferfishItems.FLAIL_VARIANTS.forEach(item -> stacks.add(item.toStack()));
        return stacks;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        return player;
    }

    @GameTest(template = "empty")
    public static void freshFishStackAndActivatedFishRemainDurable(GameTestHelper helper) {
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
            helper.assertTrue(restored.isDamageableItem() && !restored.isStackable()
                    && restored.getHoverName().equals(original.getHoverName()), "Repair lost metadata or made an activated fish stackable");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilEnchantingActivatesFishAndGrindingKeepsDurability(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var fresh : TestItems.fishVariants()) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var book = new ItemStack(Items.ENCHANTED_BOOK);
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book,
                    entries -> entries.set(enchantments.getOrThrow(Enchantments.MENDING), 1));
            var anvil = new AnvilMenu(1, player.getInventory());
            anvil.getSlot(0).set(fresh);
            anvil.getSlot(1).set(book);
            var fish = anvil.getSlot(2).getItem().copy();
            helper.assertTrue(!fish.isEmpty() && fish.isDamageableItem() && !fish.isStackable()
                    && !fish.getEnchantments().isEmpty(), "Anvil enchanting did not activate fish durability");
            player.getInventory().add(fish.copy());
            player.getInventory().add(fish.copy());
            helper.assertTrue(player.getInventory().getItem(0).getCount() == 1
                    && player.getInventory().getItem(1).getCount() == 1, "Inventory merged enchanted fish");

            PufferfishDurability.damage(fish, helper.getLevel(), null, null);
            fish.setDamageValue(0);
            helper.assertTrue(!fish.isStackable(), "Repair made an enchanted fish stackable");

            var menu = new net.minecraft.world.inventory.GrindstoneMenu(1, player.getInventory());
            menu.getSlot(0).set(fish);
            var result = menu.getSlot(2).getItem();
            helper.assertTrue(!result.isEmpty() && result.getEnchantments().isEmpty() && result.isDamageableItem() && !result.isStackable(),
                    "Disenchanting a repaired fish removed durability or allowed stacking");

            fish.enchant(enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1);
            menu.getSlot(0).set(fish.copy());
            result = menu.getSlot(2).getItem();
            helper.assertTrue(!result.isEmpty() && !result.getEnchantments().isEmpty() && !result.isStackable(),
                    "Grindstone allowed a cursed fish to stack");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meleeSeparatesOneFishWithoutDamagingTheReserve(GameTestHelper helper) {
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(Items.PUFFERFISH, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
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
    public static void fullAndPartiallyFullInventoriesDropOnlyTheOverflow(GameTestHelper helper) {
        for (var original : variants()) {
            if (!PufferfishItems.isPufferfish(original)) continue;
            original.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keepsake"));
            for (int space : new int[] {0, 4}) {
                var player = player(helper);
                var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 3));
                target.setNoAi(true);
                var stack = original.copyWithCount(64);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                for (int slot = 1; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
                if (space > 0) player.getInventory().setItem(1, original.copyWithCount(64 - space));
                player.attack(target);
                helper.assertTrue(target.getHealth() < target.getMaxHealth() && stack.getCount() == 1 && stack.getDamageValue() == 1,
                        "Inventory overflow blocked the attack or damaged more than one fish");
                var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2),
                        item -> ItemStack.isSameItemSameComponents(item.getItem(), original));
                helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 63 - space,
                        "Overflow lost fish, duplicated fish, or changed their components");
                var drop = drops.getFirst();
                helper.assertTrue(Math.abs(drop.getY() - player.getY() - 0.1) < 0.0001
                                && drop.getDeltaMovement().horizontalDistanceSqr() == 0,
                        "Overflow was thrown instead of dropped at the player's feet");
                int reserves = player.getInventory().countItem(original.getItem()) - 1;
                helper.assertTrue(reserves == (space == 0 ? 0 : 64), "Inventory space was not filled before dropping overflow");
                if (space > 0) helper.assertTrue(ItemStack.isSameItemSameComponents(player.getInventory().getItem(1), original),
                        "Stored reserves lost their components or gained damage");
                target.discard();
                drop.discard();
                player.getAdvancements().stopListening();
            }
        }
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
            helper.assertTrue(fish.getMaxStackSize() == 1 && fish.isDamageableItem()
                    && PufferfishColors.get(fish) == DyeColor.BLUE && !fish.getEnchantments().isEmpty(), "Mending lost metadata or allowed enchanted fish to stack");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void freshFishRepairInAnvilConsumesOneAndPreservesEnchantments(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        var fish = worn(new ItemStack(Items.PUFFERFISH));
        var cultivation = new PufferfishCultivation(PufferfishLevel.LEVEL_5, 0.875, 123, 456);
        fish.set(PufferfishPlease.CULTIVATION.get(), cultivation);
        fish.setDamageValue(40);
        fish.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        var menu = new AnvilMenu(1, player.getInventory());
        menu.getSlot(0).set(fish);
        menu.getSlot(1).set(new ItemStack(Items.PUFFERFISH, 4));
        var result = menu.getSlot(2).remove(1);
        helper.assertTrue(!result.isEmpty() && !result.isDamaged() && result.getMaxStackSize() == 1 && !result.getEnchantments().isEmpty(),
                "Fresh fish did not repair an enchanted fish");
        helper.assertTrue(cultivation.equals(result.get(PufferfishPlease.CULTIVATION.get())), "Anvil repair discarded cultivation");
        menu.getSlot(2).onTake(player, result);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount() == 3, "Anvil repair consumed the wrong number of fish");
        for (boolean enchanted : new boolean[] {false, true}) {
            var material = worn(new ItemStack(Items.PUFFERFISH));
            material.set(PufferfishPlease.CULTIVATION.get(), cultivation);
            if (enchanted) material.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
            menu.getSlot(0).set(fish.copy());
            menu.getSlot(1).set(material);
            helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Anvil consumed a cultivated repair material");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wornFishSurviveBucketsEntitySavingAndPlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var pristine : variants()) {
            if (!PufferfishItems.isPufferfish(pristine)) continue;
            var original = worn(pristine);
            original.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.875, 12345, 67890));
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
            helper.assertTrue(ItemStack.matches(original, stored), "Release, saving or recapture lost fish components");
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
        var cultivation = new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.625, 54321, 98765);
        netherite.set(PufferfishPlease.CULTIVATION.get(), cultivation);
        for (var addition : java.util.List.of(Items.HEAVY_CORE, Items.NETHER_STAR)) {
            for (int damage : new int[] {0, 1000}) {
                netherite.setDamageValue(damage);
                var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(PufferfishItems.UPGRADE_TEMPLATE.toStack(), netherite, new ItemStack(addition));
                var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level).orElseThrow().value();
                var result = recipe.assemble(input, level.registryAccess());
                helper.assertTrue(result.getMaxDamage() == PufferfishDurability.maximum(result)
                        && result.getMaxStackSize() == 1
                        && result.getDamageValue() == (int) Math.ceil((double) damage * result.getMaxDamage() / 2031), "Smithing reset or mis-scaled durability");
                helper.assertTrue(cultivation.equals(result.get(PufferfishPlease.CULTIVATION.get())), "Smithing lost cultivation data");
            }
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
            first.setDamageValue(10);
            second.setDamageValue(10);
            result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(!result.isDamaged() && result.isDamageableItem() && result.getMaxStackSize() == 1
                    && result.get(PufferfishPlease.COLOR.get()) == DyeColor.RED, "Full crafting repair lost durable state or dye");
            var cultivation = new PufferfishCultivation(PufferfishLevel.LEVEL_5, 0.875, 123, 456);
            first.set(PufferfishPlease.CULTIVATION.get(), cultivation);
            for (var order : java.util.List.of(java.util.List.of(first, second), java.util.List.of(second, first))) {
                var cultivatedInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, order);
                helper.assertTrue(recipe.matches(cultivatedInput, helper.getLevel())
                        && cultivation.equals(recipe.assemble(cultivatedInput, helper.getLevel().registryAccess()).get(PufferfishPlease.CULTIVATION.get())),
                        "Crafting repair discarded cultivation based on ingredient order");
            }
            for (var other : java.util.List.of(cultivation, new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.25, 12, 34))) {
                second.set(PufferfishPlease.CULTIVATION.get(), other);
                helper.assertTrue(!recipe.matches(input, helper.getLevel()) && recipe.assemble(input, helper.getLevel().registryAccess()).isEmpty(),
                        "Crafting repair consumed two cultivated fish");
            }
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
