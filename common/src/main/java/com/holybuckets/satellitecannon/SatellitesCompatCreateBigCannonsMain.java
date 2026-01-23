package com.holybuckets.satellitecannon;


import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.satellitecannon.config.TemplateConfig;
import com.holybuckets.satellitecannon.core.RemoteCannonWeaponCommon;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.UseBlockEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Main instance of the mod, initialize this class statically via commonClass
 * This class will init all major Manager instances and events for the mod
 */
public class SatellitesCompatCreateBigCannonsMain {
    private static boolean DEV_MODE = false;;
    private static TemplateConfig CONFIG;
    public static SatellitesCompatCreateBigCannonsMain INSTANCE;

    public SatellitesCompatCreateBigCannonsMain()
    {
        super();
        INSTANCE = this;
        init();
        // LoggerProject.logInit( "001000", this.getClass().getName() ); // Uncomment if you have a logging system in place
    }

    private void init()
    {

        /*
        Proxy for external APIs which are platform dependent
        this.portalApi = (PortalApi) Balm.platformProxy()
            .withFabric("com.holybuckets.challengetemple.externalapi.FabricPortalApi")
            .withForge("com.holybuckets.challengetemple.externalapi.ForgePortalApi")
            .build();
           */

        //Events
        EventRegistrar registrar = EventRegistrar.getInstance();
        //ChallengeBlockBehavior.init(registrar);


        //register local events
        registrar.registerOnBeforeServerStarted(this::onServerStarting);
        //registrar.registerOnUseBlock(this::onPlayerUseBlock);

    }

    private void onServerStarting(ServerStartingEvent e) {
        //CONFIG = Balm.getConfig().getActiveConfig(TemplateConfig.class);
        //this.DEV_MODE = CONFIG.devMode;
        this.DEV_MODE = false;
    }

    public static double V_FACTOR= 1.0;
    private static double MIN = .10;
    private static double MAX = 2.0;
    private void onPlayerUseBlock(UseBlockEvent event) {
        Level level = event.getLevel();
        if(level==null) return;
        if(level.isClientSide()) return;
        if(event.getHand()!= InteractionHand.MAIN_HAND) return;

        BlockPos pos = event.getHitResult().getBlockPos();
        if(pos==null) return;
        if(level.getBlockState(pos).getBlock().equals(Blocks.GRASS_BLOCK)) {
            if(V_FACTOR<MAX) {
                V_FACTOR += 0.1;
            } else {
                V_FACTOR = MIN;
            }
        }
        LoggerProject.logInfo( "000001", "V_FACTOR set to " + V_FACTOR);
    }


}
