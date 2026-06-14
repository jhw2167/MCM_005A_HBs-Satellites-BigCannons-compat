package com.holybuckets.satellitecannon;

import com.holybuckets.foundation.event.BalmEventRegister;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.util.ModContext;
import com.holybuckets.satellitecannon.client.ModRenderers;
import com.holybuckets.satellitecannon.core.RemoteCannonWeaponCommon;
import com.holybuckets.satellitecannon.platform.Services;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;

import java.util.ServiceLoader;

public class CommonClass {

    public static final String CREATE_BIG_CANNONS_MOD_ID = "createbigcannons";

    public static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;

        com.holybuckets.foundation.FoundationInitializers.commonInitialize();

        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {
            Constants.LOG.info("Hello to " + Constants.MOD_NAME + "!");
        }

        SatellitesCompatCreateBigCannonsMain.INSTANCE = new SatellitesCompatCreateBigCannonsMain();
        BalmEventRegister.registerEvents();

        // Only resolve the proxy if createbigcannons is present in this runtime.
        if (ModContext.getInstance().isLoaded(CREATE_BIG_CANNONS_MOD_ID)) {
            RemoteCannonWeaponCommon rmc = (RemoteCannonWeaponCommon) Balm.platformProxy().withNeoForge("com.holybuckets.satellitecannon.core.RemoteCannonWeapon"
            ).build();
            rmc.init(EventRegistrar.getInstance());
        }

        isInitialized = true;
    }

    public static void sample() {}

    public static void initClient() {
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }
}