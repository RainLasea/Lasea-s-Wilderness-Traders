package com.abysslasea.wildernesstraders.worldgen;

import com.abysslasea.wildernesstraders.entity.ModEntities;
import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.trader.TraderData;
import com.abysslasea.wildernesstraders.trader.TraderResourceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.phys.AABB;

import java.util.Set;

public class WagonPiece extends TemplateStructurePiece {
    private static final ResourceLocation WAGON_NBT = new ResourceLocation("wildernesstraders", "wagon");
    private boolean traderSpawned = false;

    public WagonPiece(StructureTemplateManager templateManager, BlockPos pos) {
        super(ModStructures.WAGON_PIECE.get(), 0, templateManager, WAGON_NBT, WAGON_NBT.toString(),
                makeSettings(), pos);
        this.setupBoundingBox(templateManager, pos, makeSettings());
    }

    public WagonPiece(StructureTemplateManager templateManager, CompoundTag nbt) {
        super(ModStructures.WAGON_PIECE.get(), nbt, templateManager,
                (resourceLocation) -> makeSettings());
        this.traderSpawned = nbt.getBoolean("TraderSpawned");
    }

    public WagonPiece(StructurePieceSerializationContext context, CompoundTag nbt) {
        this(context.structureTemplateManager(), nbt);
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(true);
    }

    private void setupBoundingBox(StructureTemplateManager templateManager, BlockPos pos, StructurePlaceSettings settings) {
        StructureTemplate template = templateManager.getOrCreate(WAGON_NBT);
        if (template != null) {
            Vec3i size = template.getSize(settings.getRotation());
            this.boundingBox = new BoundingBox(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + size.getX() - 1,
                    pos.getY() + size.getY() - 1,
                    pos.getZ() + size.getZ() - 1
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putBoolean("TraderSpawned", this.traderSpawned);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        if ("trader_spawn".equals(name)) {
            spawnTrader(level, pos, random);
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private void spawnTrader(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (this.traderSpawned) {
            return;
        }

        try {
            TraderEntity trader = new TraderEntity(ModEntities.TRADER.get(), level.getLevel());
            trader.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ());

            String profession = getRandomTraderProfession(random);
            trader.setTraderProfession(profession);

            trader.forceInitialize();

            TraderData traderData = TraderResourceManager.INSTANCE.getTrader(profession);
            if (traderData != null && trader.getCurrentMoney() <= 0) {
                trader.setCurrentMoney(traderData.getInitialMoney());
            }

            trader.setYRot(0.0F);
            trader.setYHeadRot(0.0F);

            if (trader.isPersistenceRequired() == false) {
                trader.setPersistenceRequired();
            }

            level.addFreshEntity(trader);
            this.traderSpawned = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRandomTraderProfession(RandomSource random) {
        try {
            Set<String> availableProfessions = TraderResourceManager.INSTANCE.getLoadedTraderTypes();

            if (availableProfessions.isEmpty()) {
                return "basic_trader";
            }

            String[] professionArray = availableProfessions.toArray(new String[0]);
            int randomIndex = random.nextInt(professionArray.length);
            return professionArray[randomIndex];

        } catch (Exception e) {
            e.printStackTrace();
            return "basic_trader";
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator chunkGenerator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, structureManager, chunkGenerator, random, box, chunkPos, pos);

        registerProtectedArea(level);

        if (!this.traderSpawned && !hasDataMarkers()) {
            BlockPos traderPos = findStructureCenter(level);
            if (traderPos != null) {
                spawnTrader(level, traderPos, random);
            }
        }
    }

    private void registerProtectedArea(WorldGenLevel level) {
        if (this.boundingBox != null) {
            AABB protectionArea = new AABB(
                    this.boundingBox.minX() - 1, this.boundingBox.minY() - 1, this.boundingBox.minZ() - 1,
                    this.boundingBox.maxX() + 1, this.boundingBox.maxY() + 1, this.boundingBox.maxZ() + 1
            );

            WagonProtectionManager.addProtectedArea(level.getLevel().dimension(), protectionArea);
        }
    }

    private BlockPos findStructureCenter(WorldGenLevel level) {
        if (this.boundingBox == null) return null;

        int traderX = this.boundingBox.minX() + 3;
        int traderY = this.boundingBox.minY() + 2;
        int traderZ = this.boundingBox.minZ() + 6;

        return new BlockPos(traderX, traderY, traderZ);
    }

    private boolean hasDataMarkers() {
        StructureTemplate template = this.template();
        if (template == null) {
            return false;
        }

        for (StructureTemplate.StructureBlockInfo blockInfo : template.filterBlocks(this.templatePosition(), this.placeSettings(), Blocks.STRUCTURE_VOID)) {
            if (blockInfo != null) {
                return true;
            }
        }
        return false;
    }
}