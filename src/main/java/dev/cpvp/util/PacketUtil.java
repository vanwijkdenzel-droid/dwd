package dev.cpvp.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

public final class PacketUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private PacketUtil() {}

    public static void send(Packet<?> packet) {
        var handler = mc.getNetworkHandler();
        if (handler != null) handler.sendPacket(packet);
    }

    /** Raw attack packet (ServerboundInteractPacket.attack in Mojmap) - bypasses the client click gating. */
    public static void attack(Entity target) {
        send(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
    }

    /** Swing animation + swing packet (ClientPlayerEntity#swingHand sends the packet itself). */
    public static void swing() {
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
