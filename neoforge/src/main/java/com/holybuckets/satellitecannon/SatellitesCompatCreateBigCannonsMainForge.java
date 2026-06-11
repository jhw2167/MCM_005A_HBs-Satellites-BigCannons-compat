package com.holybuckets.satellitecannon;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.satellitecannon.core.RemoteCannonWeapon;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class SatellitesCompatCreateBigCannonsMainForge {

    public SatellitesCompatCreateBigCannonsMainForge(IEventBus modEventBus) {
        // NeoForge entry point delegating Balm init through the mod event bus
        final var context = new NeoForgeLoadContext(modEventBus);
        Balm.initialize(Constants.MOD_ID, context, CommonClass::init);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BalmClient.initialize(Constants.MOD_ID, CommonClass::initClient);
        }
        RemoteCannonWeapon.init(EventRegistrar.getInstance());
    }
}
