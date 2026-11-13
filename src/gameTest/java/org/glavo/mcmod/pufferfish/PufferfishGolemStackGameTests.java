// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import org.glavo.mcmod.pufferfish.test.CuttingAssertions;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishGolemStackGameTests {
    @GameTest(template = "empty")
    public static void mixedTowerPreservesOrderCapacityAndTracking(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setShiftKeyDown(true);
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        var expected = new ArrayList<ItemStack>();
        var variants = new ArrayList<>(PufferfishVariant.ALL);
        for (int i = 0; i < 256; i++) {
            var fish = CuttingAssertions.rankedFish(variants.get(i % variants.size()), i % 11);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Tower fish " + i));
            PufferfishDurability.activate(fish);
            fish.setDamageValue(i % 7);
            expected.add(fish.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, fish);
            helper.assertTrue(player.interactOn(golem, InteractionHand.MAIN_HAND).consumesAction()
                    && player.getMainHandItem().isEmpty(), "Tower did not consume one fish");
        }
        helper.assertTrue(ItemStack.matches(golem.getItemBySlot(EquipmentSlot.HEAD), expected.getFirst()), "Stacking replaced the active fish");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && PufferfishGolemStack.decorations(golem).size() == 255,
                "Full tower consumed an item or exceeded capacity");
        var saved = new CompoundTag();
        golem.saveWithoutId(saved);
        var restored = EntityType.IRON_GOLEM.create(helper.getLevel());
        restored.load(saved);
        var receiver = EntityType.IRON_GOLEM.create(helper.getLevel());
        var tracker = new ServerEntity(helper.getLevel(), restored, 3, true, packet -> {});
        tracker.sendPairingData(FakePlayerFactory.getMinecraft(helper.getLevel()), new PacketAndPayloadAcceptor<>(packet -> {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SyncAttachmentsPayload data) {
                AttachmentSync.receiveSyncedDataAttachments(receiver, helper.getLevel().registryAccess(), data.types(), data.syncPayload());
            }
        }));
        helper.assertTrue(PufferfishGolemStack.decorations(receiver).size() == 255, "Tracking lost the tower");
        for (int i = 1; i < expected.size(); i++) {
            helper.assertTrue(ItemStack.matches(PufferfishGolemStack.decorations(receiver).get(i - 1), expected.get(i)),
                    "Tracking changed fish order or components");
        }
        for (int i = expected.size() - 1; i >= 0; i--) {
            player.getInventory().clearContent();
            helper.assertTrue(player.interactOn(restored, InteractionHand.MAIN_HAND).consumesAction()
                    && ItemStack.matches(player.getMainHandItem(), expected.get(i)), "Retrieval lost fish data or order");
        }
        helper.assertTrue(restored.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !restored.hasData(PufferfishPlease.GOLEM_FISH),
                "Empty tower retained fish or attachment data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyBottomFishActsAndNextFishReplacesBrokenHeadgear(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setShiftKeyDown(true);
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        for (var stack : List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(PufferfishItems.RADIANT.get()))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.interactOn(golem, InteractionHand.MAIN_HAND);
        }
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(golem));
        helper.assertTrue(!golem.hasEffect(MobEffects.NIGHT_VISION), "Decoration granted an equipment effect");
        var head = golem.getItemBySlot(EquipmentSlot.HEAD);
        PufferfishDurability.activate(head);
        head.setDamageValue(head.getMaxDamage() - 1);
        var attacker = EntityType.PIG.create(helper.getLevel());
        golem.hurt(golem.damageSources().mobAttack(attacker), 1);
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Active headgear did not break");
        helper.assertTrue(PufferfishGolemStack.decorations(golem).getFirst().getDamageValue() == 0,
                "Decoration took equipment wear");
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(golem));
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(golem));
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).is(PufferfishItems.RADIANT.get())
                && PufferfishGolemStack.decorations(golem).isEmpty() && golem.hasEffect(MobEffects.NIGHT_VISION),
                "Next fish did not replace broken headgear and gain its effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerRetrievalRespectsBindingAndForeignEquipment(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setShiftKeyDown(true);
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        golem.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && !golem.hasData(PufferfishPlease.GOLEM_FISH),
                "Tower overwrote foreign headgear");
        golem.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        var top = new ItemStack(PufferfishItems.IRON.get());
        top.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, top);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().isEmpty() && PufferfishGolemStack.decorations(golem).size() == 1,
                "Survival retrieval bypassed the top fish's binding curse");
        player.setGameMode(GameType.CREATIVE);
        player.interactOn(golem, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(PufferfishItems.IRON.get()) && PufferfishGolemStack.decorations(golem).isEmpty()
                && golem.getItemBySlot(EquipmentSlot.HEAD).is(Items.PUFFERFISH), "Creative retrieval removed the wrong fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerDeathDropsRespectVanishingAndMobLoot(GameTestHelper helper) {
        var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        boolean previous = rule.get();
        try {
            for (boolean mobLoot : new boolean[] {true, false}) {
                rule.set(mobLoot, helper.getLevel().getServer());
                var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
                var player = TestPlayers.serverPlayer(helper);
                player.setShiftKeyDown(true);
                var expected = new ArrayList<ItemStack>();
                for (int i = 0; i < 4; i++) {
                    var fish = new ItemStack(Items.PUFFERFISH);
                    PufferfishDurability.activate(fish);
                    fish.set(DataComponents.CUSTOM_NAME, Component.literal("Death drop " + i));
                    if (i == 2) fish.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.VANISHING_CURSE), 1);
                    else expected.add(fish.copy());
                    player.setItemInHand(InteractionHand.MAIN_HAND, fish);
                    player.interactOn(golem, InteractionHand.MAIN_HAND);
                }
                var drops = new ArrayList<ItemStack>();
                Consumer<LivingDropsEvent> listener = event -> {
                    if (event.getEntity() == golem) {
                        event.getDrops().stream().map(item -> item.getItem()).filter(PufferfishItems::isPufferfish).forEach(drops::add);
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, listener);
                try {
                    golem.hurt(golem.damageSources().genericKill(), 1000);
                } finally {
                    NeoForge.EVENT_BUS.unregister(listener);
                }
                helper.assertTrue(drops.size() == (mobLoot ? expected.size() : 0), "Tower drops ignored mob loot or vanishing");
                if (mobLoot) for (var fish : expected) helper.assertTrue(drops.stream().anyMatch(drop -> ItemStack.matches(drop, fish)),
                        "Death lost fish components or duplicated a fish");
                helper.assertTrue(PufferfishGolemStack.decorations(golem).isEmpty(), "Death retained tower items");
            }
        } finally {
            rule.set(previous, helper.getLevel().getServer());
        }
        helper.succeed();
    }
}
