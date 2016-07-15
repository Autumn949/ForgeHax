package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.Angle;
import com.matt.forgehax.util.PlayerUtils;
import com.matt.forgehax.util.ProjectileUtils;
import com.matt.forgehax.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoProjectile extends ToggleMod {
    public AutoProjectile(String modName, boolean defaultValue, String description, int key) {
        super(modName, defaultValue, description, key);
    }

    @SubscribeEvent
    public void onSendingPacket(PacketEvent.SendEvent.Pre event) {
        EntityPlayer localPlayer = MC.thePlayer;
        if (!PlayerUtils.isProjectileTargetAcquired() &&
                !PlayerUtils.isFakeAnglesActive()) {
            if (event.getPacket() instanceof CPacketPlayerDigging &&
                    ((CPacketPlayerDigging) event.getPacket()).getAction().equals(CPacketPlayerDigging.Action.RELEASE_USE_ITEM) &&
                    !Utils.OUTGOING_PACKET_IGNORE_LIST.contains(event.getPacket())) {
                ItemStack heldItem = localPlayer.getHeldItemMainhand();
                RayTraceResult trace = localPlayer.rayTrace(9999.D, 0.f);
                if (heldItem != null &&
                        getNetworkManager() != null &&
                        trace != null &&
                        ProjectileUtils.isBow(heldItem)) {
                    Angle oldViewAngles = PlayerUtils.getViewAngles();
                    // send new angles
                    PlayerUtils.sendRotatePacket(
                            ProjectileUtils.getBestPitch(heldItem, trace.hitVec),
                            oldViewAngles.getYaw()
                    );
                    // tell server we let go of bow
                    Packet usePacket = new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN);
                    // add to ignore list
                    Utils.OUTGOING_PACKET_IGNORE_LIST.add(usePacket);
                    getNetworkManager().sendPacket(usePacket);
                    // revert back to old angles
                    PlayerUtils.sendRotatePacket(oldViewAngles);
                    event.setCanceled(true);
                }
            } else if (event.getPacket() instanceof CPacketPlayerTryUseItem &&
                    ((CPacketPlayerTryUseItem) event.getPacket()).getHand().equals(EnumHand.MAIN_HAND) &&
                    !Utils.OUTGOING_PACKET_IGNORE_LIST.contains(event.getPacket())) {
                ItemStack heldItem = localPlayer.getHeldItemMainhand();
                RayTraceResult trace = localPlayer.rayTrace(9999.D, 0.f);
                if(heldItem != null &&
                        trace != null &&
                        ProjectileUtils.isThrowable(heldItem) &&
                        !ProjectileUtils.isBow(heldItem)) {
                    // send server our new view angles
                    PlayerUtils.sendRotatePacket(
                            ProjectileUtils.getBestPitch(heldItem, trace.hitVec),
                            PlayerUtils.getViewAngles().getYaw()
                    );
                    // tell server we let go of bow
                    Packet usePacket = new CPacketPlayerTryUseItem(((CPacketPlayerTryUseItem) event.getPacket()).getHand());
                    // add to ignore list
                    Utils.OUTGOING_PACKET_IGNORE_LIST.add(usePacket);
                    getNetworkManager().sendPacket(usePacket);
                    // revert back to the old view angles
                    PlayerUtils.sendRotatePacket(PlayerUtils.getViewAngles());
                    // cancel this event (wont send the packet)
                    event.setCanceled(true);
                }
            }
        }
    }
}