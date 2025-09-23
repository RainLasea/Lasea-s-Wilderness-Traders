package com.abysslasea.wildernesstraders.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "wildernesstraders", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WagonProtection {

    private static final Map<ResourceKey<Level>, Set<AABB>> PROTECTED_AREAS = new HashMap<>();

    public static void addProtectedArea(ResourceKey<Level> dimension, AABB area) {
        PROTECTED_AREAS.computeIfAbsent(dimension, k -> new HashSet<>()).add(area);
    }

    public static boolean isProtected(Level level, BlockPos pos) {
        Set<AABB> areas = PROTECTED_AREAS.get(level.dimension());
        if (areas == null) return false;

        return areas.stream().anyMatch(aabb ->
                aabb.contains(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static boolean canInteract(Level level, BlockPos pos) {
        if (!isProtected(level, pos)) return true;

        var blockState = level.getBlockState(pos);
        return blockState.getBlock() instanceof DoorBlock &&
                blockState.is(Blocks.SPRUCE_DOOR);
    }

    public static void clearDimension(ResourceKey<Level> dimension) {
        PROTECTED_AREAS.remove(dimension);
    }

    public static int getProtectedAreasCount(ResourceKey<Level> dimension) {
        Set<AABB> areas = PROTECTED_AREAS.get(dimension);
        return areas != null ? areas.size() : 0;
    }

    public static void removeProtectedArea(ResourceKey<Level> dimension, AABB area) {
        Set<AABB> areas = PROTECTED_AREAS.get(dimension);
        if (areas != null) {
            areas.remove(area);
            if (areas.isEmpty()) {
                PROTECTED_AREAS.remove(dimension);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        if (isProtected(level, event.getPos())) {
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

        if (isProtected(level, event.getPos())) {
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

        if (isProtected(level, event.getPos()) &&
                !canInteract(level, event.getPos())) {
            event.setResult(Event.Result.DENY);
            event.getEntity().displayClientMessage(
                    Component.translatable("wildernesstraders.message.wagon_interact_blocked"), true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (isProtected(level, event.getPos())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(
                    Component.translatable("wildernesstraders.message.wagon_attack_protected"), true);
        }
    }
}