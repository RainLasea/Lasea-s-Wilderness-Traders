package com.abysslasea.wildernesstraders.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Map;

public class ModStructureData {
    public static final ResourceKey<Structure> WAGON_STRUCTURE = ResourceKey.create(
            Registries.STRUCTURE, new ResourceLocation("wildernesstraders", "wagon"));

    public static final ResourceKey<StructureSet> WAGON_STRUCTURE_SET = ResourceKey.create(
            Registries.STRUCTURE_SET, new ResourceLocation("wildernesstraders", "wagon_structures"));

    public static void bootstrap(BootstapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(WAGON_STRUCTURE, new WagonStructure(
                new Structure.StructureSettings(
                        biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                        Map.of(),
                        GenerationStep.Decoration.SURFACE_STRUCTURES,
                        TerrainAdjustment.BEARD_THIN
                ),
                ConstantHeight.of(VerticalAnchor.absolute(64))
        ));
    }

    public static void bootstrapStructureSet(BootstapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        context.register(WAGON_STRUCTURE_SET, new StructureSet(
                structures.getOrThrow(WAGON_STRUCTURE),
                new RandomSpreadStructurePlacement(
                        6,
                        12,
                        RandomSpreadType.LINEAR,
                        12345
                )
        ));
    }
}