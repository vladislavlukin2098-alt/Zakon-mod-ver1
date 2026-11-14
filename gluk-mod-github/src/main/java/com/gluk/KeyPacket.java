package com.gluk;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: нажата клавиша артефакта. */
public class KeyPacket {
    public static final int PORTAL = 0;
    public static final int BLINK = 1;

    private final int action;

    public KeyPacket(int action) { this.action = action; }

    public static void encode(KeyPacket msg, FriendlyByteBuf buf) { buf.writeByte(msg.action); }

    public static KeyPacket decode(FriendlyByteBuf buf) { return new KeyPacket(buf.readByte()); }

    public static void handle(KeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player != null) ArtifactLogic.onKey(player, msg.action);
        });
        c.setPacketHandled(true);
    }
}
