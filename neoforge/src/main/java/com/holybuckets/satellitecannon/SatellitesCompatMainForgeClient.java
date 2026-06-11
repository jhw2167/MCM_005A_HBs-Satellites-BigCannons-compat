package com.holybuckets.satellitecannon;

import com.holybuckets.satellitecannon.client.CommonClassClient;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import static com.holybuckets.satellitecannon.Constants.MOD_ID;

@Mod(value = MOD_ID, dist = Dist.CLIENT)
public class SatellitesCompatMainForgeClient {

    public SatellitesCompatMainForgeClient(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        BalmClient.initialize(MOD_ID, context, CommonClassClient::initClient);
    }

}
