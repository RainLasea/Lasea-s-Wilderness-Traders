package com.abysslasea.wildernesstraders.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class WagonProtectionManager {
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
}