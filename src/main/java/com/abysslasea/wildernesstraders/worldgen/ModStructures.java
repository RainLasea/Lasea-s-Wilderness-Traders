package com.abysslasea.wildernesstraders.worldgen;

import com.abysslasea.wildernesstraders.WildernessTraders;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, WildernessTraders.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, WildernessTraders.MODID);

    public static final RegistryObject<StructureType<WagonStructure>> WAGON_STRUCTURE =
            STRUCTURE_TYPES.register("wagon_structure", () -> () -> WagonStructure.CODEC);

    public static final RegistryObject<StructurePieceType> WAGON_PIECE =
            STRUCTURE_PIECE_TYPES.register("wagon_piece", () -> WagonPiece::new);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECE_TYPES.register(eventBus);
    }
}