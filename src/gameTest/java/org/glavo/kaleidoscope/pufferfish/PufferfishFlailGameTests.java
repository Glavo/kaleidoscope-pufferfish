// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;

/// Exercises flail crafting, combat, launch ownership, and item conservation.
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishFlailGameTests {
    /// Prevents construction of the test holder.
    private PufferfishFlailGameTests() {}

    /// Creates a stationary survival player holding the supplied weapon above the fixture floor.
    private static Player player(GameTestHelper helper, ItemStack stack) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2))));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setNoGravity(true);
        return player;
    }

    private static void launch(GameTestHelper helper, PufferfishFlailHead head) {
        helper.assertTrue(head.isQueued(), "Release skipped the queued swing");
        for (int i = 0; i < 100 && head.isSpinning() && !head.isRemoved(); i++) {
            head.setOldPosAndRot();
            head.tickCount++;
            head.tick();
        }
        helper.assertTrue(!head.isSpinning() && !head.isRemoved() && !head.isReturning(), "Queued swing did not launch");
    }

    /// Constructs the requested diagonal recipe or its horizontal mirror.
    private static CraftingInput input(ItemStack fish, boolean mirrored) {
        var items = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        items.set(mirrored ? 0 : 2, fish);
        items.set(4, new ItemStack(Items.CHAIN));
        items.set(mirrored ? 8 : 6, new ItemStack(Items.CHAIN));
        return CraftingInput.of(3, 3, items);
    }

    /// Verifies all recipes, dye colors, component preservation, and invalid inputs.
    @GameTest(template = "empty")
    public static void diagonalRecipesPreserveFishComponents(GameTestHelper helper) {
        var variants = new ArrayList<ItemStack>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        variants.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        variants.add(new ItemStack(PufferfishItems.HEAVY.get()));
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PUNCH);
        for (var fish : variants) {
            fish.setCount(8);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Meteor"));
            EnchantmentHelper.updateEnchantments(fish, values -> values.set(enchantment, 2));
            var original = fish.copy();
            for (boolean mirrored : new boolean[] {false, true}) {
                var grid = input(fish, mirrored);
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow().value();
                var result = recipe.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(result.is(PufferfishItems.isHeavy(fish) ? PufferfishItems.HEAVY_FLAIL.get()
                        : PufferfishItems.isNetherite(fish) ? PufferfishItems.NETHERITE_FLAIL.get() : PufferfishItems.FLAIL.get())
                        && result.getCount() == 1 && result.getMaxStackSize() == 1, "Wrong flail recipe output");
                helper.assertTrue(result.get(KaleidoscopePufferfish.COLOR.get()) == PufferfishColors.get(fish)
                        && result.getHoverName().equals(fish.getHoverName()) && result.getEnchantments().getLevel(enchantment) == 2,
                        "Flail recipe lost fish components");
                helper.assertTrue(!result.has(DataComponents.ITEM_NAME), "Flail retained a generated fish name");
                helper.assertTrue(ItemStack.matches(original, fish) && recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                        "Flail recipe mutated its ingredient or duplicated materials");
                helper.assertTrue(!recipe.canCraftInDimensions(2, 2), "Flail recipe fitted a player crafting grid");
            }
        }
        for (var wrong : List.of(new ItemStack(Items.COD), new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(PufferfishItems.FLAIL.get()))) {
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input(wrong, false), helper.getLevel()).isEmpty(),
                    "Flail recipe accepted an invalid fish ingredient");
        }
        var invalid = new ArrayList<>(input(new ItemStack(Items.PUFFERFISH), false).items());
        invalid.set(0, new ItemStack(Items.CHAIN));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, invalid), helper.getLevel()).isEmpty(),
                "Flail recipe accepted an extra ingredient");
        helper.succeed();
    }

    /// Verifies melee damage and poison without treating a flail as wearable fish or engine fuel.
    @GameTest(template = "empty")
    public static void flailsDealMeleeDamageWithoutFishEquipmentBehavior(GameTestHelper helper) {
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            var stack = new ItemStack(item);
            var player = player(helper, stack);
            for (int i = 0; i < 40; i++) player.tick();
            player.setOnGround(true);
            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 3, 3));
            target.setNoAi(true);
            boolean netherite = PufferfishItems.hasNetheriteCombat(new ItemStack(item));
            helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == (netherite ? 12 : 6)
                    && Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - (PufferfishItems.hasHeavyCombat(stack) ? 0.6 : 1)) < 0.000001,
                    "Incorrect flail melee attributes");
            player.attack(target);
            var poison = target.getEffect(MobEffects.POISON);
            helper.assertTrue(target.getHealth() == (netherite ? 8 : 14) && poison != null
                    && poison.getDuration() == 120 && poison.getAmplifier() == (netherite ? 1 : 0), "Flail melee damage or poison failed");
            helper.assertTrue(!PufferfishItems.isPufferfish(stack), "Flail was classified as a fish ingredient");
            player.setItemSlot(EquipmentSlot.HEAD, stack);
            helper.assertTrue(!PufferfishHeadgear.isWearing(player), "Flail granted fish headgear effects");
            target.discard();
        }
        helper.succeed();
    }

    /// Verifies successful launches preserve the item and share a cooldown across all variants.
    @GameTest(template = "empty")
    public static void launchPreservesTheWeaponAndRespectsCooldown(GameTestHelper helper) {
        for (var hand : InteractionHand.values()) for (var item : List.of(
                PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            var stack = new ItemStack(item);
            stack.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.BLUE);
            var player = player(helper, ItemStack.EMPTY);
            player.setItemInHand(hand, stack);
            var original = stack.copy();
            var result = stack.use(helper.getLevel(), player, hand);
            helper.assertTrue(result.getResult().consumesAction() && player.isUsingItem()
                    && PufferfishFlailItem.activeHead(player) != null && PufferfishFlailItem.activeHead(player).isCharging()
                    && !player.getCooldowns().isOnCooldown(stack.getItem()), "Starting charge launched or cooled down the weapon");
            for (int i = 0; i < PufferfishFlailItem.chargeTicks(stack); i++) player.tick();
            player.releaseUsingItem();
            var heads = helper.getLevel().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(3));
            helper.assertTrue(result.getResult().consumesAction() && heads.size() == 1, "Flail did not launch exactly one head");
            var head = heads.getFirst();
            helper.assertTrue(!player.getCooldowns().isOnCooldown(item), "Queued swing started cooldown before release");
            launch(helper, head);
            helper.assertTrue(head.getOwner() == player && head.getHand() == hand && ItemStack.matches(head.getItem(), original)
                    && ItemStack.matches(stack, original), "Launch lost ownership or changed the held item");
            helper.assertTrue(player.getCooldowns().isOnCooldown(PufferfishItems.FLAIL.get())
                    && player.getCooldowns().isOnCooldown(PufferfishItems.NETHERITE_FLAIL.get())
                    && player.getCooldowns().isOnCooldown(PufferfishItems.HEAVY_FLAIL.get()), "Flail variants did not share cooldown");
            helper.assertTrue(stack.use(helper.getLevel(), player, hand).getResult() == InteractionResult.FAIL, "Cooldown allowed another launch");
            head.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heavyFlailAcceptsOnlyFlailEnchantments(GameTestHelper helper) {
        var stack = new ItemStack(PufferfishItems.HEAVY_FLAIL.get());
        var player = player(helper, stack);
        var supported = List.of(Enchantments.DENSITY, Enchantments.BREACH, Enchantments.PUNCH, Enchantments.SMITE,
                Enchantments.BANE_OF_ARTHROPODS, Enchantments.VANISHING_CURSE, Enchantments.THORNS, Enchantments.CHANNELING);
        var unsupported = List.of(Enchantments.AQUA_AFFINITY, Enchantments.LOYALTY, Enchantments.WIND_BURST, Enchantments.BINDING_CURSE);
        for (var keys : List.of(supported, unsupported)) {
            for (var key : keys) {
                var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
                var book = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(book, entries -> entries.set(enchantment, 1));
                var menu = new net.minecraft.world.inventory.AnvilMenu(1, player.getInventory());
                menu.getSlot(0).set(stack.copy());
                menu.getSlot(1).set(book);
                var output = menu.getSlot(2).getItem();
                helper.assertTrue(supported.contains(key) ? output.is(stack.getItem()) && output.getEnchantments().getLevel(enchantment) == 1
                        : output.isEmpty(), "Heavy flail accepted the wrong enchantment set: " + key);
            }
        }
        helper.succeed();
    }

    /// Verifies cancellation leaves both the held weapon and cooldown unchanged.
    @GameTest(template = "empty")
    public static void canceledLaunchDoesNotConsumeOrCoolDown(GameTestHelper helper) {
        var stack = new ItemStack(PufferfishItems.FLAIL.get());
        var player = player(helper, stack);
        Consumer<EntityJoinLevelEvent> cancel = event -> {
            if (event.getEntity() instanceof PufferfishFlailHead) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            for (int i = 0; i < PufferfishFlailItem.chargeTicks(stack); i++) player.tick();
            player.releaseUsingItem();
            helper.assertTrue(result.getResult() == InteractionResult.FAIL && !player.isUsingItem() && stack.getCount() == 1
                    && helper.getLevel().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(3)).isEmpty()
                    && !player.getCooldowns().isOnCooldown(stack.getItem()), "Canceled launch consumed or cooled down the weapon");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.succeed();
    }

    /// Verifies a visible return flight reaches the owner before cooldown ends without duplicating the weapon.
    @GameTest(template = "empty")
    public static void tetherReturnsWithoutDroppingAnotherWeapon(GameTestHelper helper) {
        var player = player(helper, new ItemStack(PufferfishItems.FLAIL.get()));
        var head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        // Start near the tether limit at the latest outward tick to exercise the full return budget.
        head.setPos(player.getEyePosition().add(0, 5.9, 0));
        head.tickCount = 7;
        head.setDeltaMovement(0, 1.2, 0);
        helper.getLevel().addFreshEntity(head);
        boolean returned = false;
        int returnTicks = 0;
        for (int i = 0; i < PufferfishFlailItem.COOLDOWN && !head.isRemoved(); i++) {
            head.tickCount++;
            head.tick();
            returned |= head.isReturning();
            if (head.isReturning()) returnTicks++;
            helper.assertTrue(head.position().distanceTo(player.getEyePosition()) <= PufferfishFlailHead.REACH, "Head exceeded its tether range");
        }
        helper.assertTrue(returned && head.isRemoved() && player.getMainHandItem().getCount() == 1, "Head did not return to its retained weapon");
        helper.assertTrue(returnTicks >= 10, "Distant head returned too quickly to show the retraction");
        helper.assertTrue(head.position().distanceTo(PufferfishFlailMotion.grip(player, InteractionHand.MAIN_HAND, player.getYRot(), 1)) <= 0.21
                && head.tickCount < PufferfishFlailItem.COOLDOWN, "Head expired before reaching the owner or outlasted the cooldown");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8),
                entity -> PufferfishItems.isFlail(entity.getItem())).isEmpty(), "Return duplicated the weapon");
        head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.getLevel().addFreshEntity(head);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        head.tick();
        helper.assertTrue(head.isRemoved(), "Unequipping a flail left its head active");
        helper.succeed();
    }

    /// Verifies the flying head damages once and does not damage again while returning.
    @GameTest(template = "empty")
    public static void flyingHeadHitsOnceAndReturns(GameTestHelper helper) {
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            var player = player(helper, new ItemStack(item));
            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 3, 4));
            target.setNoAi(true);
            target.setNoGravity(true);
            var head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            head.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
            head.setDeltaMovement(0, 0, 3);
            helper.getLevel().addFreshEntity(head);
            head.tick();
            boolean netherite = PufferfishItems.hasNetheriteCombat(new ItemStack(item));
            helper.assertTrue(head.isReturning() && target.getHealth() == (netherite ? 5 : 17)
                    && target.hasEffect(MobEffects.POISON), "Flying head did not hit and start returning");
            float health = target.getHealth();
            target.invulnerableTime = 0;
            head.setPos(target.position().add(0, 0.8, 0));
            head.tick();
            helper.assertTrue(target.getHealth() == health, "Returning head dealt another hit");
            head.discard();
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chargedDamageScalesAndSurvivesSaving(GameTestHelper helper) {
        var density = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            for (int rank : new int[] {0, 5}) {
                var stack = new ItemStack(item);
                stack.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.CYAN);
                if (rank > 0) EnchantmentHelper.updateEnchantments(stack, entries -> entries.set(density, rank));
                int duration = PufferfishFlailItem.chargeTicks(stack);
                for (int elapsed : new int[] {3, duration / 2, duration, duration * 2}) {
                    var player = player(helper, stack.copy());
                    var held = player.getMainHandItem();
                    held.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    for (int tick = 0; tick < elapsed; tick++) player.tick();
                    player.releaseUsingItem();
                    var heads = helper.getLevel().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(3));
                    helper.assertTrue(heads.size() == 1, "Charged release did not spawn one head");
                    var head = heads.getFirst();
                    launch(helper, head);
                    var tag = new net.minecraft.nbt.CompoundTag();
                    head.addAdditionalSaveData(tag);
                    var restored = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND, 0);
                    restored.readAdditionalSaveData(tag);
                    // Mock players are not registered for UUID lookup after loading.
                    restored.setOwner(player);
                    head.discard();
                    var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 3, 4));
                    target.setNoAi(true);
                    target.setNoGravity(true);
                    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                    target.setHealth(100);
                    restored.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
                    restored.setDeltaMovement(0, 0, 3);
                    helper.getLevel().addFreshEntity(restored);
                    restored.tick();
                    float baseline = (PufferfishItems.hasNetheriteCombat(stack) ? 10 : 2) + (rank == 0 ? 0 : 3);
                    float expected = baseline * 1.5F * Math.min(1, (float) elapsed / duration);
                    helper.assertTrue(Math.abs(100 - target.getHealth() - expected) < 0.001
                            && restored.isReturning() && target.hasEffect(MobEffects.POISON),
                            "Charge damage or saved charge was incorrect: elapsed=" + elapsed + ", expected=" + expected
                                    + ", actual=" + (100 - target.getHealth()) + ", removed=" + restored.isRemoved()
                                    + ", owner=" + (restored.getOwner() == player) + ", charge=" + tag.getFloat("Charge"));
                    restored.discard();
                    target.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightChangesChargeTimeWithoutChangingRange(GameTestHelper helper) {
        var density = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        double referenceRange = -1;
        int normalDuration = PufferfishFlailItem.chargeTicks(new ItemStack(PufferfishItems.FLAIL.get()));
        helper.assertTrue(PufferfishFlailItem.chargeTicks(new ItemStack(PufferfishItems.NETHERITE_FLAIL.get())) > normalDuration,
                "Netherite flail did not take longer to charge");
        helper.assertTrue(PufferfishFlailItem.chargeTicks(new ItemStack(PufferfishItems.HEAVY_FLAIL.get()))
                > PufferfishFlailItem.chargeTicks(new ItemStack(PufferfishItems.NETHERITE_FLAIL.get())),
                "Heavy flail did not take longer to charge than netherite");
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            int previousDuration = 0;
            for (int rank : new int[] {0, 1, 5}) {
                var stack = new ItemStack(item);
                if (rank > 0) EnchantmentHelper.updateEnchantments(stack, entries -> entries.set(density, rank));
                int duration = PufferfishFlailItem.chargeTicks(stack);
                helper.assertTrue(duration > previousDuration, "Density did not increase charge time");
                previousDuration = duration;
                var player = player(helper, stack);
                // Keep the flight above the test structure's barrier enclosure.
                player.setPos(player.position().add(0, 12, 0));
                player.setXRot(-90);
                stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                for (int tick = 0; tick < duration; tick++) player.tick();
                player.releaseUsingItem();
                var head = helper.getLevel().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(3)).getFirst();
                helper.assertTrue(head.getOwner() == player && player.isAlive()
                        && ItemStack.isSameItemSameComponents(player.getMainHandItem(), head.getItem()),
                        "Range fixture has an invalid owner or weapon: owner=" + (head.getOwner() == player)
                                + ", health=" + player.getHealth() + ", velocity=" + head.getDeltaMovement());
                launch(helper, head);
                var initialVelocity = head.getDeltaMovement();
                double range = 0;
                for (int tick = 0; tick < PufferfishFlailItem.COOLDOWN && !head.isRemoved(); tick++) {
                    head.tickCount++;
                    head.tick();
                    range = Math.max(range, head.position().distanceTo(player.getEyePosition()));
                }
                helper.assertTrue(head.isRemoved() && range > 4 && range <= PufferfishFlailHead.REACH,
                        "Charged flail exceeded its tether or failed to return: range=" + range + ", removed=" + head.isRemoved()
                                + ", initial=" + initialVelocity + ", velocity=" + head.getDeltaMovement() + ", player=" + player.position());
                if (referenceRange < 0) referenceRange = range;
                helper.assertTrue(Math.abs(range - referenceRange) < 0.001, "Weight changed the charged throw range");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shortTapsAndInterruptedChargesDoNotLaunch(GameTestHelper helper) {
        for (int scenario = 0; scenario < 3; scenario++) {
            var stack = new ItemStack(PufferfishItems.FLAIL.get());
            var player = player(helper, stack);
            stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            int ticks = scenario == 0 ? 2 : PufferfishFlailItem.chargeTicks(stack);
            for (int tick = 0; tick < ticks; tick++) player.tick();
            if (scenario == 1) player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (scenario == 2) player.stopUsingItem();
            else player.releaseUsingItem();
            var active = PufferfishFlailItem.activeHead(player);
            if (active != null) active.tick();
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(3)).isEmpty()
                    && !player.getCooldowns().isOnCooldown(stack.getItem()) && stack.getCount() == 1,
                    "Short or interrupted charge launched, consumed, or cooled down the weapon");
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void releaseFollowsTheOrbitAndLocksDamage(GameTestHelper helper) {
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            for (var hand : InteractionHand.values()) for (float pitch : new float[] {-90, -35, 0, 60, 90}) {
                var stack = new ItemStack(item);
                for (int age : new int[] {3, 10, 20, 45, 80}) {
                    var player = player(helper, ItemStack.EMPTY);
                    player.setItemInHand(hand, stack);
                    player.setYRot(47);
                    player.setXRot(pitch);
                    player.setPos(player.position().add(0, 12, 0));
                    stack.use(helper.getLevel(), player, hand);
                    var head = PufferfishFlailItem.activeHead(player);
                    for (int tick = 0; tick < age; tick++) {
                        player.tick();
                        head.tickCount++;
                        head.tick();
                    }
                    helper.assertTrue(head.isCharging() && !head.isRemoved(), "Long charge expired or launched early");
                    player.releaseUsingItem();
                    helper.assertTrue(PufferfishFlailItem.activeHead(player) == head && head.isQueued(), "Release replaced the spinning head");
                    var saved = new net.minecraft.nbt.CompoundTag();
                    head.addAdditionalSaveData(saved);
                    helper.assertTrue(Math.abs(saved.getFloat("Charge") - PufferfishFlailItem.charge(stack, age)) < 0.0001,
                            "Release did not freeze damage at input time");
                    float gate = PufferfishFlailMotion.releaseAge(stack, age, pitch);
                    helper.assertTrue(gate >= age && gate - age < 100, "Invalid release window");
                    var tangent = PufferfishFlailMotion.tangent(stack, gate, 47);
                    helper.assertTrue(tangent.dot(player.getLookAngle().normalize()) > 0.99999, "Release is not tangent to the aimed trajectory: pitch=" + pitch + ", age=" + age + ", gate=" + gate + ", yaw=" + player.getYRot() + ", actualPitch=" + player.getXRot() + ", dot=" + tangent.dot(player.getLookAngle()));
                    player.setYRot(-90);
                    player.setXRot(0);
                    launch(helper, head);
                    var expected = PufferfishFlailMotion.orbit(player, hand, stack, gate, 47, 1);
                    helper.assertTrue(head.position().distanceTo(expected) < 0.0001
                            && head.getDeltaMovement().distanceTo(tangent.scale(1.2)) < 0.0001, "Launch jumped away from the release point");
                    head.addAdditionalSaveData(saved);
                    helper.assertTrue(Math.abs(saved.getFloat("Charge") - PufferfishFlailItem.charge(stack, age)) < 0.0001,
                            "Waiting for release increased damage");
                    head.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void queuedReleaseCancelsOnUnequipAndSurvivesSaving(GameTestHelper helper) {
        var stack = new ItemStack(PufferfishItems.HEAVY_FLAIL.get());
        var player = player(helper, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        stack.use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        for (int tick = 0; tick < 10; tick++) player.tick();
        var head = PufferfishFlailItem.activeHead(player);
        player.releaseUsingItem();
        helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
                "Queued release allowed a second swing");
        var tag = new net.minecraft.nbt.CompoundTag();
        head.addAdditionalSaveData(tag);
        var restored = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        restored.readAdditionalSaveData(tag);
        restored.setOwner(player);
        helper.assertTrue(restored.isQueued() && restored.getHand() == InteractionHand.OFF_HAND,
                "Saved queued release lost state or hand");
        head.discard();
        helper.getLevel().addFreshEntity(restored);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        restored.tick();
        helper.assertTrue(restored.isRemoved() && !player.getCooldowns().isOnCooldown(stack.getItem()),
                "Unequipping a queued flail did not cancel safely");
        helper.succeed();
    }

}
