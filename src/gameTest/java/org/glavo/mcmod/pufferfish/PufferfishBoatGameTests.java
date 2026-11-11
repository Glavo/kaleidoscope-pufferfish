// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.PufferfishBoat;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishBoatGameTests {
    @GameTest(template = "empty")
    public static void everySkinHasAMatchingBoat(GameTestHelper helper) {
        int skins = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof PufferfishSkinBlock)) continue;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(PufferfishPlease.MODID)) continue;
            var boat = PufferfishBoats.VARIANTS.get(id.getPath() + "_boat");
            helper.assertTrue(boat != null, "Missing boat for skin: " + id);
            helper.assertTrue(boat.entity().get().create(helper.getLevel()).getDropItem() == boat.item().get(),
                    "Boat drops a different skin variant: " + id);
            skins++;
        }
        helper.assertTrue(skins == PufferfishBoats.VARIANTS.size(), "Skin and boat variants are not one-to-one");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void heavyKayakSinksToTheFloorAndRetainsPassengers(GameTestHelper helper) {
        for (var pos : BlockPos.betweenClosed(0, 0, 0, 6, 5, 6)) {
            helper.setBlock(pos, pos.getY() == 0 || pos.getX() == 0 || pos.getX() == 6 || pos.getZ() == 0 || pos.getZ() == 6
                    ? Blocks.STONE : Blocks.WATER);
        }
        var origin = helper.absolutePos(BlockPos.ZERO);
        var heavy = PufferfishBoats.VARIANTS.get("heavy_pufferfish_skin_boat").entity().get().create(helper.getLevel());
        var ordinary = PufferfishBoats.VARIANTS.get("pufferfish_skin_boat").entity().get().create(helper.getLevel());
        heavy.setPos(origin.getX() + 2.25, origin.getY() + 6.2, origin.getZ() + 3.5);
        ordinary.setPos(origin.getX() + 4.75, origin.getY() + 5.6, origin.getZ() + 3.5);
        helper.getLevel().addFreshEntity(heavy);
        helper.getLevel().addFreshEntity(ordinary);
        var passenger = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 6, 3));
        passenger.setNoAi(true);
        passenger.startRiding(heavy, true);
        helper.runAtTickTime(30, () -> helper.assertTrue(heavy.getY() < origin.getY() + 3,
                "Heavy kayak did not sink after entering water"));
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(!heavy.isRemoved() && Math.abs(heavy.getY() - origin.getY() - 1) < 0.05,
                    "Heavy kayak did not settle on the underwater floor");
            helper.assertTrue(passenger.getVehicle() == heavy, "Submerged heavy kayak ejected its passenger");
            helper.assertTrue(ordinary.getY() > origin.getY() + 5, "Ordinary kayak lost its buoyancy: " + ordinary.position()
                    + ", water=" + helper.getLevel().getFluidState(origin.offset(4, 5, 3)) + ", heavy=" + ordinary.isHeavy());
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            helper.assertTrue(heavy.interact(player, InteractionHand.MAIN_HAND).consumesAction() && player.getVehicle() == heavy,
                    "Heavy kayak could not be boarded underwater");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void variantsCraftSaveCarryPassengersAndDropMatchingBoats(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var entry : PufferfishBoats.VARIANTS.entrySet()) {
            var skin = new ItemStack(BuiltInRegistries.ITEM.get(PufferfishPlease.id(entry.getKey().replace("_boat", ""))));
            var input = CraftingInput.of(3, 2, List.of(skin, ItemStack.EMPTY, skin, skin, skin, skin));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
            var result = recipe.assemble(input, level.registryAccess());
            helper.assertTrue(result.is(entry.getValue().item().get()) && result.is(ItemTags.BOATS) && result.getMaxStackSize() == 1,
                    "Incorrect skin boat recipe or item tag");
            var boat = entry.getValue().entity().get().create(level);
            boat.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            boat.setCustomName(Component.literal("Saved skin boat"));
            var saved = new CompoundTag();
            boat.save(saved);
            var restored = (PufferfishBoat) EntityType.loadEntityRecursive(saved, level, entity -> entity);
            helper.assertTrue(restored != null && restored.skinBoatName().equals(entry.getKey())
                    && restored.getPickResult().is(result.getItem()) && restored.getCustomName().equals(boat.getCustomName()),
                    "Skin boat lost its variant or name when saved");
            level.addFreshEntity(boat);
            var first = helper.makeMockPlayer(GameType.SURVIVAL);
            var second = helper.makeMockPlayer(GameType.SURVIVAL);
            var third = helper.makeMockPlayer(GameType.SURVIVAL);
            helper.assertTrue(boat.interact(first, InteractionHand.MAIN_HAND).consumesAction()
                    && boat.interact(second, InteractionHand.MAIN_HAND).consumesAction()
                    && !third.startRiding(boat) && boat.getControllingPassenger() == first, "Skin boat seating differed from vanilla");
            first.stopRiding();
            second.stopRiding();
            boat.hurt(level.damageSources().playerAttack(first), 5);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            helper.assertTrue(boat.isRemoved() && drops.size() == 1 && drops.getFirst().getItem().is(result.getItem())
                    && drops.getFirst().getItem().getHoverName().getString().equals("Saved skin boat"), "Broken boat lost its variant or name");
            drops.forEach(ItemEntity::discard);
            var falling = entry.getValue().entity().get().create(level);
            falling.setPos(boat.position());
            level.addFreshEntity(falling);
            falling.causeFallDamage(5, 1, level.damageSources().fall());
            drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            helper.assertTrue(falling.isRemoved() && drops.size() == 1 && drops.getFirst().getItem().is(result.getItem()), "Fallen boat dropped wooden materials");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemsPlaceNamedBoatsAndRespectCreativeConsumption(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var p : BlockPos.betweenClosed(0, 1, 0, 4, 1, 4)) helper.setBlock(p, Blocks.WATER);
        for (var variant : PufferfishBoats.VARIANTS.values()) for (var mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
            var player = helper.makeMockPlayer(mode);
            player.getAbilities().instabuild = mode == GameType.CREATIVE;
            var pos = helper.absolutePos(new BlockPos(2, 2, 0));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            player.setYRot(0);
            player.setXRot(45);
            var stack = new ItemStack(variant.item().get());
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Placed boat"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(stack.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Boat item failed to place on water");
            var boats = level.getEntitiesOfClass(PufferfishBoat.class, new AABB(pos).inflate(5));
            helper.assertTrue(boats.size() == 1 && boats.getFirst().getType() == variant.entity().get()
                    && boats.getFirst().getName().getString().equals("Placed boat")
                    && stack.isEmpty() == (mode == GameType.SURVIVAL), "Boat placement changed its variant, name or consumption");
            boats.forEach(PufferfishBoat::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dispensersPlaceBoatsInWaterAndDropItemsOnLand(GameTestHelper helper) {
        var level = helper.getLevel();
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        var state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH);
        helper.setBlock(relative, state);
        var source = new BlockSource(level, pos, state, (DispenserBlockEntity) level.getBlockEntity(pos));
        for (var variant : PufferfishBoats.VARIANTS.values()) {
            helper.setBlock(relative.south(), Blocks.WATER);
            var stack = new ItemStack(variant.item().get());
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Dispensed boat"));
            new PufferfishBoatItem.Dispenser(variant.entity()).execute(source, stack);
            var boats = level.getEntitiesOfClass(PufferfishBoat.class, new AABB(pos).inflate(4));
            helper.assertTrue(stack.isEmpty() && boats.size() == 1 && boats.getFirst().getType() == variant.entity().get()
                    && boats.getFirst().getName().getString().equals("Dispensed boat"), "Dispenser spawned the wrong boat");
            boats.forEach(PufferfishBoat::discard);
            helper.setBlock(relative.south(), Blocks.STONE);
            stack = new ItemStack(variant.item().get());
            new PufferfishBoatItem.Dispenser(variant.entity()).execute(source, stack);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4));
            helper.assertTrue(stack.isEmpty() && drops.size() == 1 && drops.getFirst().getItem().is(variant.item().get()), "Dry dispenser did not drop the boat item");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }
}
