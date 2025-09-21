package com.abysslasea.wildernesstraders;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.dialogue.DialogueManager;
import com.abysslasea.wildernesstraders.network.DialoguePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onEntityRightClick(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (target instanceof TraderEntity trader) {
            String npcName = trader.getCleanDisplayName();
            String traderId = trader.getTraderName();

            try {
                trader.triggerTalkAnimation();
            } catch (Exception e) {
                System.err.println("动画触发失败: " + e.getMessage());
            }

            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    DialogueManager.INSTANCE.syncToPlayer(serverPlayer, target, traderId);

                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            DialoguePacket.openGui(target.getId(), npcName, traderId));
                } catch (Exception e) {
                    System.err.println("对话同步失败: " + e.getMessage());
                }
            }
            event.setCanceled(true);
        }
    }
}