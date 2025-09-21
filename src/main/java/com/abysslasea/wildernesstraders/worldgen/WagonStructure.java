package com.abysslasea.wildernesstraders.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class WagonStructure extends Structure {
    public static final Codec<WagonStructure> CODEC = RecordCodecBuilder.<WagonStructure>mapCodec(instance ->
            instance.group(
                    WagonStructure.settingsCodec(instance),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight)
            ).apply(instance, WagonStructure::new)).codec();

    private final HeightProvider startHeight;

    public WagonStructure(StructureSettings settings, HeightProvider startHeight) {
        super(settings);
        this.startHeight = startHeight;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!checkLocation(context)) {
            return Optional.empty();
        }

        BlockPos blockpos = new BlockPos(
                context.chunkPos().getMinBlockX(),
                this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor())),
                context.chunkPos().getMinBlockZ()
        );

        if (!isValidGround(context, blockpos)) {
            return Optional.empty();
        }

        return Optional.of(new GenerationStub(blockpos, (structurePiecesBuilder) -> {
            generatePieces(structurePiecesBuilder, context, blockpos);
        }));
    }

    private boolean checkLocation(GenerationContext context) {
        return context.random().nextFloat() < 0.8f;
    }

    private boolean isValidGround(GenerationContext context, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                int groundHeight = context.chunkGenerator().getFirstOccupiedHeight(
                        checkPos.getX(), checkPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState()
                );

                if (Math.abs(groundHeight - pos.getY()) > 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos pos) {
        builder.addPiece(new WagonPiece(context.structureTemplateManager(), pos));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.WAGON_STRUCTURE.get();
    }
}