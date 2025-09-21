package com.abysslasea.wildernesstraders;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.entity.TraderWorldData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TraderCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trader")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("refresh")
                        .then(Commands.literal("nearby")
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 50.0))
                                        .executes(TraderCommands::refreshNearbyTraders))
                                .executes(context -> refreshNearbyTraders(context, 10.0)))
                        .then(Commands.literal("target")
                                .then(Commands.argument("trader", EntityArgument.entity())
                                        .executes(TraderCommands::refreshTargetTrader)))
                        .then(Commands.literal("all")
                                .executes(TraderCommands::refreshAllTraders)))
                .then(Commands.literal("info")
                        .then(Commands.literal("nearby")
                                .executes(TraderCommands::infoNearbyTraders))
                        .then(Commands.argument("trader", EntityArgument.entity())
                                .executes(TraderCommands::infoTargetTrader))));
    }

    private static int refreshNearbyTraders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        double radius = DoubleArgumentType.getDouble(context, "radius");
        return refreshNearbyTraders(context, radius);
    }

    private static int refreshNearbyTraders(CommandContext<CommandSourceStack> context, double radius) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        List<TraderEntity> nearbyTraders = player.level().getEntitiesOfClass(
                TraderEntity.class,
                new AABB(player.blockPosition()).inflate(radius)
        );

        if (nearbyTraders.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.wildernesstraders.refresh.nearby.none", radius),
                    false
            );
            return 0;
        }

        TraderWorldData worldData = TraderWorldData.get(player.level());
        int refreshedCount = 0;

        for (TraderEntity trader : nearbyTraders) {
            worldData.forceRefreshTrader(trader);
            refreshedCount++;
        }

        int finalRefreshedCount = refreshedCount;
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.refresh.nearby.success", finalRefreshedCount, radius),
                true
        );

        return refreshedCount;
    }

    private static int refreshTargetTrader(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "trader");

        if (!(entity instanceof TraderEntity trader)) {
            context.getSource().sendFailure(
                    Component.translatable("commands.wildernesstraders.refresh.target.not_trader")
            );
            return 0;
        }

        TraderWorldData worldData = TraderWorldData.get(trader.level());
        worldData.forceRefreshTrader(trader);

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.refresh.target.success",
                        trader.getDisplayName(), trader.getTraderName()),
                true
        );

        return 1;
    }

    private static int refreshAllTraders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        List<TraderEntity> allTraders = player.level().getEntitiesOfClass(
                TraderEntity.class,
                new AABB(
                        player.level().getWorldBorder().getMinX(),
                        player.level().getMinBuildHeight(),
                        player.level().getWorldBorder().getMinZ(),
                        player.level().getWorldBorder().getMaxX(),
                        player.level().getMaxBuildHeight(),
                        player.level().getWorldBorder().getMaxZ()
                )
        );

        if (allTraders.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.wildernesstraders.refresh.all.none"),
                    false
            );
            return 0;
        }

        TraderWorldData worldData = TraderWorldData.get(player.level());
        int refreshedCount = 0;

        for (TraderEntity trader : allTraders) {
            worldData.forceRefreshTrader(trader);
            refreshedCount++;
        }

        int finalRefreshedCount = refreshedCount;
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.refresh.all.success", finalRefreshedCount),
                true
        );

        return refreshedCount;
    }

    private static int infoNearbyTraders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        List<TraderEntity> nearbyTraders = player.level().getEntitiesOfClass(
                TraderEntity.class,
                new AABB(player.blockPosition()).inflate(10.0)
        );

        if (nearbyTraders.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.wildernesstraders.info.nearby.none"),
                    false
            );
            return 0;
        }

        TraderWorldData worldData = TraderWorldData.get(player.level());

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.nearby.header"),
                false
        );

        for (TraderEntity trader : nearbyTraders) {
            String traderUUID = trader.getTraderUUID();
            int money = trader.getCurrentMoney();
            int daysUntilRefresh = worldData.getDaysUntilRefresh(traderUUID, trader.level().getGameTime());

            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.wildernesstraders.info.nearby.entry",
                            trader.getDisplayName(),
                            trader.getTraderName(),
                            money,
                            daysUntilRefresh),
                    false
            );
        }

        return nearbyTraders.size();
    }

    private static int infoTargetTrader(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "trader");

        if (!(entity instanceof TraderEntity trader)) {
            context.getSource().sendFailure(
                    Component.translatable("commands.wildernesstraders.info.target.not_trader")
            );
            return 0;
        }

        TraderWorldData worldData = TraderWorldData.get(trader.level());
        String traderUUID = trader.getTraderUUID();

        int money = trader.getCurrentMoney();
        int daysUntilRefresh = worldData.getDaysUntilRefresh(traderUUID, trader.level().getGameTime());
        int tradeCount = worldData.getTraderTrades(traderUUID).size();

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.target.header"),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.target.name", trader.getDisplayName()),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.target.type", trader.getTraderName()),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.target.money", money),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.target.trades", tradeCount),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.wildernesstraders.info.target.refresh", daysUntilRefresh),
                false
        );

        return 1;
    }
}