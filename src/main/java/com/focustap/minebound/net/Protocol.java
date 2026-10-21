package com.focustap.minebound.net;

import com.focustap.minebound.Minebound;
import com.focustap.minebound.rpg.*;
import com.focustap.minebound.server.CharacterService;
import com.google.gson.Gson;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Common networking has no references to net.minecraft.client or client screen classes. */
public final class Protocol {
    public static final Gson JSON = new Gson();
    public static Consumer<Snapshot> clientReceiver = value -> {};
    public record Snapshot(CharacterProfile profile, Battle battle, String mode, String notice, long recoveryRemaining) {}
    public record Create(String name, String race, String startingClass, int[] attributes) {}
    public record Command(String op, String data, float x, float y, boolean focus) implements CustomPacketPayload {
        public static final Type<Command> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Minebound.MOD_ID,"command"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Command> CODEC = StreamCodec.of((b,p) -> {
            b.writeUtf(p.op,32); b.writeUtf(p.data,4096); b.writeFloat(p.x); b.writeFloat(p.y); b.writeBoolean(p.focus);
        }, b -> new Command(b.readUtf(32),b.readUtf(4096),b.readFloat(),b.readFloat(),b.readBoolean()));
        @Override public Type<Command> type() { return TYPE; }
    }
    public record Update(String json) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Minebound.MOD_ID,"update"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Update> CODEC = StreamCodec.of((b,p) -> b.writeUtf(p.json,120000),
                b -> new Update(b.readUtf(120000)));
        @Override public Type<Update> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(Command.TYPE,Command.CODEC,(p,c) -> {
            if (c.player() instanceof ServerPlayer player) CharacterService.command(player,p);
        });
        registrar.playToClient(Update.TYPE,Update.CODEC,(p,c) -> clientReceiver.accept(JSON.fromJson(p.json,Snapshot.class)));
    }
    public static void send(String op, String data) { send(op,data,0,0,false); }
    public static void send(String op, String data, float x, float y, boolean focus) {
        PacketDistributor.sendToServer(new Command(op,data,x,y,focus));
    }
    public static void sync(ServerPlayer player, Snapshot snapshot) {
        PacketDistributor.sendToPlayer(player,new Update(JSON.toJson(snapshot)));
    }
}
