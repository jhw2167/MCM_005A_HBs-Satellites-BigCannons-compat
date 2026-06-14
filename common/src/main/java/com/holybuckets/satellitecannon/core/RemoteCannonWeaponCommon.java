package com.holybuckets.satellitecannon.core;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.satellite.block.be.TargetReceiverBlockEntity;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import static com.holybuckets.satellitecannon.SatellitesCompatCreateBigCannonsMain.V_FACTOR;

public interface RemoteCannonWeaponCommon {


    String WEAPON_ID = "[CBC: {pos}]";
    int MIN_RADIUS = 10;
    int MAX_ITERS = 10000;

    ResourceLocation CBC_CANNON_MOUNT_ID = HBUtil.LOC("createbigcannons", "cannon_mount");
    ResourceLocation CBC_FIXED_MOUNT_ID = HBUtil.LOC("createbigcannons", "fixed_cannon_mount");
    ResourceLocation CBC_SOLID_SHOT_ID = HBUtil.LOC("createbigcannons", "solid_shot");
    ResourceLocation CBC_SOLID_SHOT_TYPE_ID = HBUtil.LOC("createbigcannons", "shot");
    ResourceLocation CBC_DROP_MORTAR_SHELL_TYPE_ID = HBUtil.LOC("createbigcannons", "drop_mortar_shell");

    // Pitch correction lookup vs error fraction.
    double[][] DIST_ERROR_TABLE = {
        {0.01, Math.toRadians(0.001)},
        {0.02, Math.toRadians(0.002)},
        {0.03, Math.toRadians(0.01)},
        {0.04, Math.toRadians(0.025)},
        {0.05, Math.toRadians(0.05)},
        {0.07, Math.toRadians(1.0)},
        {0.10, Math.toRadians(1.25)},
        {0.15, Math.toRadians(1.5)},
        {0.20, Math.toRadians(1.75)},
        {0.30, Math.toRadians(2.0)},
        {0.50, Math.toRadians(2.5)},
        {100,  Math.toRadians(3.0)},
    };


    void init(EventRegistrar registrar);
    void onServerStarting(ServerStartingEvent event);
    void cannonOnTargetSet(TargetReceiverBlockEntity receiver, BlockEntity blockEntity);
    void cannonOnFire(TargetReceiverBlockEntity receiver, BlockEntity blockEntity);


    default Vec3 getDirectionFromAngles(float pitch, float yaw) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3(x, y, z);
    }


    default Double simulateLaunchDist(double pitchRads, double v0, double formDrag, double airTime,
                                      double density, double gravity, double startPos, double targetPos, int attempt) {
        double dt = 0.05;
        double vty = Math.sin(pitchRads) * v0 * dt;
        double vtx = Math.cos(pitchRads) * v0 * dt;
        double xDist = 0;
        double yDist = 0;

        double speed = Math.sqrt(vtx * vtx + vty * vty);
        double dragMagnitude = formDrag * density * speed;
        dragMagnitude = Math.min(dragMagnitude, speed);
        double accX = -(vtx / speed) * dragMagnitude;
        double accY = -(vty / speed) * dragMagnitude - gravity;

        for (int t = 0; t < airTime * 20; t++) {
            xDist += vtx;
            yDist += vty;

            vtx += accX * V_FACTOR;
            vty += accY * V_FACTOR;

            if ((vty < 0) && (startPos + yDist < targetPos - 1)) break;

            speed = Math.sqrt(vtx * vtx + vty * vty);
            dragMagnitude = formDrag * density * speed;
            dragMagnitude = Math.min(dragMagnitude, speed);
            accX = -(vtx / speed) * dragMagnitude;
            accY = -(vty / speed) * dragMagnitude - gravity;
        }
        return xDist;
    }
}