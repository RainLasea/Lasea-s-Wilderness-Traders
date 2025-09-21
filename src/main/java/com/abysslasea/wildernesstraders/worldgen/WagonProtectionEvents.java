package com.abysslasea.wildernesstraders.worldgen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "wildernesstraders", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WagonProtectionEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        if (WagonProtectionManager.isProtected(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getPlayer() != null) {
                event.getPlayer().displayClientMessage(
                        Component.translatable("wildernesstraders.message.wagon_break_protected"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        if (WagonProtectionManager.isProtected(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                player.displayClientMessage(
                        Component.translatable("wildernesstraders.message.wagon_place_blocked"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (WagonProtectionManager.isProtected(level, event.getPos()) &&
                !WagonProtectionManager.canInteract(level, event.getPos())) {
            event.setResult(Event.Result.DENY);
            event.getEntity().displayClientMessage(
                    Component.translatable("wildernesstraders.message.wagon_interact_blocked"), true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (WagonProtectionManager.isProtected(level, event.getPos())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(
                    Component.translatable("wildernesstraders.message.wagon_attack_protected"), true);
        }
    }
}