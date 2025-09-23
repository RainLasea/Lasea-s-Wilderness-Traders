package com.abysslasea.wildernesstraders.entity;

import com.abysslasea.wildernesstraders.TraderConfig;
import com.abysslasea.wildernesstraders.trader.TraderData;
import com.abysslasea.wildernesstraders.trader.TraderResourceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class TraderEntity extends PathfinderMob implements GeoAnimatable {

    private static final EntityDataAccessor<String> TRADER_PROFESSION =
            SynchedEntityData.defineId(TraderEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DISPLAY_NAME =
            SynchedEntityData.defineId(TraderEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_TALKING =
            SynchedEntityData.defineId(TraderEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String NBT_TRADER_PROFESSION = "TraderProfession";
    private static final String NBT_DISPLAY_NAME = "DisplayName";
    private static final String NBT_MONEY_COUNT = "MoneyCount";
    private static final String NBT_INITIALIZED = "Initialized";
    private static final String NBT_NAME_SEED = "NameSeed";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long lastTalkTime = 0;
    private static final long TALK_ANIMATION_DURATION = 2000;
    private boolean hasIdleAnimation = true;
    private boolean hasWalkAnimation = true;
    private boolean hasTalkAnimation = true;

    public TraderEntity(EntityType<? extends TraderEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRADER_PROFESSION, "");
        this.entityData.define(DISPLAY_NAME, "");
        this.entityData.define(IS_TALKING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isTalking() &&
                System.currentTimeMillis() - this.lastTalkTime > TALK_ANIMATION_DURATION) {
            this.setTalking(false);
        }

        if (!this.level().isClientSide()) {
            if (!isInitialized()) {
                initializeTrader();
            }

            if (this.tickCount % 100 == 0) {
                List<Player> nearbyPlayers = this.level().getEntitiesOfClass(
                        Player.class,
                        new AABB(this.blockPosition()).inflate(32.0D)
                );

                if (!nearbyPlayers.isEmpty()) {
                    TraderWorldData.get(this.level()).checkAndUpdateTrader(this);
                }
            }
        }
    }

    public boolean isInitialized() {
        return this.getPersistentData().getBoolean(NBT_INITIALIZED);
    }

    public void forceInitialize() {
        if (!isInitialized()) {
            initializeTrader();
        }
    }

    private void initializeTrader() {
        String traderProfession = getTraderProfession();
        if (traderProfession.isEmpty()) {
            setTraderProfession("basic_trader");
            traderProfession = "basic_trader";
        }

        TraderData traderData = TraderResourceManager.INSTANCE.getTrader(traderProfession);
        if (traderData == null) {
            traderData = TraderResourceManager.INSTANCE.getDefaultTrader();
        }

        if (getTraderDisplayName().isEmpty()) {
            generateAndSetDisplayName();
        }

        if (!this.getPersistentData().contains(NBT_MONEY_COUNT)) {
            setCurrentMoney(traderData.getInitialMoney());
        }

        this.getPersistentData().putBoolean(NBT_INITIALIZED, true);
        TraderWorldData.get(this.level()).registerTrader(this);
    }

    public void generateAndSetDisplayName() {
        String existingName = getTraderDisplayName();
        if (existingName != null && !existingName.isEmpty()) {
            return;
        }

        long nameSeed = getNameSeed();
        String generatedName = TraderConfig.INSTANCE.generateName(nameSeed);

        setTraderDisplayName(generatedName);

        Component nameComponent = Component.literal(generatedName);
        this.setCustomName(nameComponent);
        this.setCustomNameVisible(false);
    }

    private long getNameSeed() {
        if (this.getPersistentData().contains(NBT_NAME_SEED)) {
            return this.getPersistentData().getLong(NBT_NAME_SEED);
        }

        long seed = (long) this.blockPosition().getX() * 31L +
                (long) this.blockPosition().getY() * 17L +
                (long) this.blockPosition().getZ() * 13L +
                (long) this.getId() * 7L;

        this.getPersistentData().putLong(NBT_NAME_SEED, seed);
        return seed;
    }

    public String getTraderProfession() {
        String profession = this.entityData.get(TRADER_PROFESSION);
        if (profession.isEmpty()) {
            profession = this.getPersistentData().getString(NBT_TRADER_PROFESSION);
            if (!profession.isEmpty()) {
                this.entityData.set(TRADER_PROFESSION, profession);
            }
        }
        return profession;
    }

    public void setTraderProfession(String profession) {
        if (profession == null) {
            profession = "";
        }
        this.entityData.set(TRADER_PROFESSION, profession);
        this.getPersistentData().putString(NBT_TRADER_PROFESSION, profession);
    }

    public String getTraderDisplayName() {
        String name = this.entityData.get(DISPLAY_NAME);
        if (name == null || name.isEmpty()) {
            name = this.getPersistentData().getString(NBT_DISPLAY_NAME);
            if (name != null && !name.isEmpty()) {
                this.entityData.set(DISPLAY_NAME, name);
            }
        }
        return name != null ? name : "";
    }

    public void setTraderDisplayName(String displayName) {
        if (displayName == null) {
            displayName = "";
        }
        this.entityData.set(DISPLAY_NAME, displayName);
        this.getPersistentData().putString(NBT_DISPLAY_NAME, displayName);
    }

    @Override
    public Component getDisplayName() {
        String displayName = getTraderDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            Component result = Component.literal(displayName);
            return result;
        }
        return super.getDisplayName();
    }

    public String getCleanDisplayName() {
        if (!isInitialized()) {
            forceInitialize();
        }

        String name = getTraderDisplayName();
        if (name != null && !name.isEmpty() &&
                !name.contains("translation") &&
                !name.contains("literal") &&
                !name.contains("key=") &&
                !name.startsWith("TranslatableComponent") &&
                !name.startsWith("LiteralComponent")) {
            return name;
        }

        generateAndSetDisplayName();
        name = getTraderDisplayName();
        return name;
    }

    public String getTraderName() {
        return getTraderProfession();
    }

    public void setTraderName(String traderName) {
        setTraderProfession(traderName);
    }

    public int getCurrentMoney() {
        return this.getPersistentData().getInt(NBT_MONEY_COUNT);
    }

    public void setCurrentMoney(int amount) {
        this.getPersistentData().putInt(NBT_MONEY_COUNT, Math.max(0, amount));
    }

    public void addMoney(int amount) {
        if (amount > 0) {
            setCurrentMoney(getCurrentMoney() + amount);
        }
    }

    public void subtractMoney(int amount) {
        if (amount > 0) {
            setCurrentMoney(getCurrentMoney() - amount);
        }
    }

    public boolean hasMoney(int amount) {
        return getCurrentMoney() >= amount;
    }

    public boolean isTalking() {
        return this.entityData.get(IS_TALKING);
    }

    public void setTalking(boolean talking) {
        this.entityData.set(IS_TALKING, talking);
        if (talking) {
            this.lastTalkTime = System.currentTimeMillis();
        }
    }

    public void triggerTalkAnimation() {
        if (this.hasTalkAnimation) {
            this.setTalking(true);
            this.lastTalkTime = System.currentTimeMillis();
        }
    }

    public void setAnimationAvailability(boolean hasIdle, boolean hasWalk, boolean hasTalk) {
        this.hasIdleAnimation = hasIdle;
        this.hasWalkAnimation = hasWalk;
        this.hasTalkAnimation = hasTalk;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString(NBT_TRADER_PROFESSION, getTraderProfession());
        compound.putString(NBT_DISPLAY_NAME, getTraderDisplayName());
        compound.putInt(NBT_MONEY_COUNT, getCurrentMoney());
        compound.putBoolean(NBT_INITIALIZED, isInitialized());
        compound.putBoolean("IsTalking", this.isTalking());
        compound.putBoolean("HasIdleAnimation", this.hasIdleAnimation);
        compound.putBoolean("HasWalkAnimation", this.hasWalkAnimation);
        compound.putBoolean("HasTalkAnimation", this.hasTalkAnimation);

        if (this.getPersistentData().contains(NBT_NAME_SEED)) {
            compound.putLong(NBT_NAME_SEED, this.getPersistentData().getLong(NBT_NAME_SEED));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.contains(NBT_TRADER_PROFESSION)) {
            setTraderProfession(compound.getString(NBT_TRADER_PROFESSION));
        }
        if (compound.contains(NBT_DISPLAY_NAME)) {
            setTraderDisplayName(compound.getString(NBT_DISPLAY_NAME));
        }
        if (compound.contains(NBT_MONEY_COUNT)) {
            setCurrentMoney(compound.getInt(NBT_MONEY_COUNT));
        }
        if (compound.contains(NBT_INITIALIZED)) {
            this.getPersistentData().putBoolean(NBT_INITIALIZED, compound.getBoolean(NBT_INITIALIZED));
        }
        if (compound.contains("IsTalking")) {
            this.setTalking(compound.getBoolean("IsTalking"));
        }
        if (compound.contains("HasIdleAnimation")) {
            this.hasIdleAnimation = compound.getBoolean("HasIdleAnimation");
        }
        if (compound.contains("HasWalkAnimation")) {
            this.hasWalkAnimation = compound.getBoolean("HasWalkAnimation");
        }
        if (compound.contains("HasTalkAnimation")) {
            this.hasTalkAnimation = compound.getBoolean("HasTalkAnimation");
        }
        if (compound.contains(NBT_NAME_SEED)) {
            this.getPersistentData().putLong(NBT_NAME_SEED, compound.getLong(NBT_NAME_SEED));
        }
    }

    public static TraderEntity createTrader(Level level, String profession) {
        TraderEntity trader = new TraderEntity(ModEntities.TRADER.get(), level);
        trader.setTraderProfession(profession);
        return trader;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public String getTraderUUID() {
        return this.getUUID().toString();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        try {
            if (this.isTalking() && this.hasTalkAnimation) {
                try {
                    animationState.getController().setAnimation(RawAnimation.begin().then("talk", Animation.LoopType.PLAY_ONCE));
                    return PlayState.CONTINUE;
                } catch (Exception e) {
                    this.hasTalkAnimation = false;
                }
            }

            if (animationState.isMoving() && this.hasWalkAnimation) {
                try {
                    animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
                    return PlayState.CONTINUE;
                } catch (Exception e) {
                    this.hasWalkAnimation = false;
                }
            }

            if (this.hasIdleAnimation) {
                try {
                    animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                    return PlayState.CONTINUE;
                } catch (Exception e) {
                    this.hasIdleAnimation = false;
                }
            }

            return PlayState.STOP;

        } catch (Exception e) {
            return PlayState.STOP;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return this.tickCount;
    }
}