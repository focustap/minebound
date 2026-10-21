package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid=Minebound.MOD_ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class ClientWiring {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> Protocol.clientReceiver = ClientEntry::receive);
    }
}
