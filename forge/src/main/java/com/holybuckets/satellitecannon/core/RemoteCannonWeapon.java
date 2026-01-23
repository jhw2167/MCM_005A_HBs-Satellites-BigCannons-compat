package com.holybuckets.satellitecannon.core;

import com.holybuckets.foundation.console.Messager;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.satellite.LoggerProject;
import com.holybuckets.satellite.block.be.TargetReceiverBlockEntity;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.*;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBehavior;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.drop_mortar.DropMortarEndBlock;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.autocannon.config.InertAutocannonProjectilePropertiesHandler;
import rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakAutocannonProjectilePropertiesHandler;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.InertBigCannonProjectilePropertiesHandler;
import rbasamoyai.createbigcannons.munitions.big_cannon.drop_mortar_shell.DropMortarShellProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCannonPropellantBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.solid_shot.SolidShotProjectile;
import rbasamoyai.createbigcannons.munitions.config.DimensionMunitionPropertiesHandler;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

import java.util.ArrayList;
import java.util.List;

import static com.holybuckets.foundation.HBUtil.BlockUtil;
import static com.holybuckets.satellitecannon.SatellitesCompatCreateBigCannonsMain.V_FACTOR;
import static rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock.ASSEMBLY_POWERED;

public class RemoteCannonWeapon {

    private static String WEAPON_ID = "[CBC: {pos}]";
    private static Messager MSGR;

    private static ResourceLocation CBC_CANNON_MOUNT_ID = new ResourceLocation("createbigcannons", "cannon_mount");
    static BlockEntityType<CannonMountBlockEntity> CBC_CANNON_MOUNT_TYPE;
    private static ResourceLocation CBC_SOLID_SHOT_ID = new ResourceLocation("createbigcannons", "solid_shot");
    static Block CBC_SOLID_SHOT;
    private static ResourceLocation  CBC_SOLID_SHOT_TYPE_ID = new ResourceLocation("createbigcannons", "shot");
    static EntityType<SolidShotProjectile> CBC_SOLID_SHOT_TYPE;
    //new constant for drop_mortar_shell
    private static ResourceLocation  CBC_DROP_MORTAR_SHELL_TYPE_ID = new ResourceLocation("createbigcannons", "drop_mortar_shell");
    static EntityType<DropMortarShellProjectile> CBC_DROP_MORTAR_SHELL_TYPE;

    static InertAutocannonProjectilePropertiesHandler CBC_AUTOCANNON_PROPS;
    static InertBigCannonProjectilePropertiesHandler CBC_BIGCANNON_PROPS;
    static FlakAutocannonProjectilePropertiesHandler FLAK_AUTOCANNON_PROPS;

    public static void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(RemoteCannonWeapon::onServerStarting);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        Registry<BlockEntityType> beRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BLOCK_ENTITY_TYPE);
        Registry<Block> blockRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BLOCK);
        Registry<EntityType<?>> entityRegistry = event.getServer().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        CBC_CANNON_MOUNT_TYPE  = beRegistry.get(CBC_CANNON_MOUNT_ID);
        CBC_SOLID_SHOT_TYPE = (EntityType<SolidShotProjectile>) entityRegistry.get(CBC_SOLID_SHOT_TYPE_ID);
        CBC_DROP_MORTAR_SHELL_TYPE = (EntityType<DropMortarShellProjectile>) entityRegistry.get(CBC_DROP_MORTAR_SHELL_TYPE_ID);

        CBC_AUTOCANNON_PROPS = CBCMunitionPropertiesHandlers.INERT_AUTOCANNON_PROJECTILE;
        CBC_BIGCANNON_PROPS = CBCMunitionPropertiesHandlers.INERT_BIG_CANNON_PROJECTILE;
        FLAK_AUTOCANNON_PROPS = CBCMunitionPropertiesHandlers.FLAK_AUTOCANNON;

        TargetReceiverBlockEntity.addNeighborBlockEntityWeapons(CBC_CANNON_MOUNT_TYPE, RemoteCannonWeapon::cannonOnFire, RemoteCannonWeapon::mountOnTargetSet);
        MSGR = Messager.getInstance();
    }

    public static int MIN_RADIUS = 10;

    private static void mountOnTargetSet(TargetReceiverBlockEntity receiver, BlockEntity blockEntity) {
        CannonMountBlockEntity be = (CannonMountBlockEntity) blockEntity;
        PitchOrientedContraptionEntity cannonAngler = be.getContraption();
        if (cannonAngler == null) return;

        if( cannonAngler.getContraption() instanceof MountedAutocannonContraption autocannon) {
            autoCannonOnTargetSet(receiver, (CannonMountBlockEntity) blockEntity, autocannon);
        } else if( cannonAngler.getContraption() instanceof AbstractMountedCannonContraption bigCannon) {
            cannonOnTargetSet(receiver, (CannonMountBlockEntity) blockEntity, bigCannon);
        }

    }

    private static void autoCannonOnTargetSet(TargetReceiverBlockEntity receiver, CannonMountBlockEntity be, MountedAutocannonContraption autoCannon)
    {
        PitchOrientedContraptionEntity cannonAngler = be.getContraption();
        Player p = receiver.getPlayerFiredWeapon();
        BlockPos targetPos = receiver.getUiTargetBlockPos();
        BlockPos mountPos = be.getBlockPos();
        if (targetPos == null || mountPos == null) return;

        if (BlockUtil.distanceSqr(targetPos, mountPos) < MIN_RADIUS * MIN_RADIUS) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            MSGR.sendBottomActionHint(p, id + ": Not in Range!");
            return;
        }

        // atan2(-x, z) gives: North (0°), East (90°), South (180°), West (270°)
        int dx = targetPos.getX() - mountPos.getX();
        int dz = targetPos.getZ() - mountPos.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));

        if (yaw < 0) {
            yaw += 360;
        }

        BlockPos endPos = autoCannon.getStartPos();
        Vec3 angles;
        if(autoCannon.presentBlockEntities.get(endPos) instanceof IBigCannonBlockEntity) {
            angles = calculateCannonAngles(be, autoCannon, endPos, targetPos);
        } else {
            return;
        }

        if(angles == null ) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            MSGR.sendBottomActionHint(p, id + ": Target out of range!");
            return;
        }
        LoggerProject.logInfo("020005", "Estimated Landing Position : " + angles);

        be.setYaw((float) angles.x);
        be.setPitch((float) angles.y);
        cannonAngler.setYRot((float) angles.x);
        be.notifyUpdate();

    }


    private static void cannonOnTargetSet(TargetReceiverBlockEntity receiver, CannonMountBlockEntity be, AbstractMountedCannonContraption cannon)
    {
        PitchOrientedContraptionEntity cannonAngler = be.getContraption();
        Player p = receiver.getPlayerFiredWeapon();
        BlockPos targetPos = receiver.getUiTargetBlockPos();
        BlockPos mountPos = be.getBlockPos();
        if (targetPos == null || mountPos == null) return;

        if (BlockUtil.distanceSqr(targetPos, mountPos) < MIN_RADIUS * MIN_RADIUS) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            MSGR.sendBottomActionHint(p, id + ": Not in Range!");
            return;
        }

        // atan2(-x, z) gives: North (0°), East (90°), South (180°), West (270°)
        int dx = targetPos.getX() - mountPos.getX();
        int dz = targetPos.getZ() - mountPos.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));

        if (yaw < 0) {
            yaw += 360;
        }

        BlockPos endPos = cannon.getStartPos().relative(cannonAngler.getInitialOrientation().getOpposite());
        Vec3 angles;
        //= new Vec3(cannonAngler.yaw, cannonAngler.pitch, 0);
        if(cannon.presentBlockEntities.get(endPos) instanceof IBigCannonBlockEntity) {
            angles = calculateCannonAngles(be, cannon, endPos, targetPos);
        } else {
            return;
        }

        if(angles == null ) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            MSGR.sendBottomActionHint(p, id + ": Target out of range!");
            return;
        }
        LoggerProject.logInfo("020005", "Estimated Landing Position : " + angles);

        be.setYaw((float) angles.x);
        be.setPitch((float) angles.y);
        cannonAngler.setYRot((float) angles.x);
        be.notifyUpdate();

    }

    private static void cannonOnFire(TargetReceiverBlockEntity receiver, BlockEntity blockEntity) {
        CannonMountBlockEntity be = (CannonMountBlockEntity) blockEntity;
        BlockPos targetPos = receiver.getUiTargetBlockPos();
        PitchOrientedContraptionEntity mountedContraption = be.getContraption();
        if (mountedContraption == null) return;
        if (!be.isRunning()) return;
        Level level = receiver.getLevel();
        BlockState state = be.getBlockState();
        boolean prevAssemblyPowered = state.getValue(ASSEMBLY_POWERED);
        be.onRedstoneUpdate(prevAssemblyPowered, true, true, false, 15);
        LoggerProject.logInfo("020001", "Cannon Fire: ");
    }

    //maps [n][0] (error fraction off to dist) to [n][1] (additional pitch)
    private static double[][] DIST_ERROR_TABLE = {

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
        {100, Math.toRadians(3.0)},
    };
    /**
     * Calculates the estimated landing position of a cannon projectile using ideal kinematics.
     * Does not account for drag - this is a simplified ballistic calculation.
     *
     * @param mount The CannonMountBlockEntity
     * @param cannon The AbstractMountedCannonContraption
     * @param endPos The BlockPos at the end of the cannon (muzzle position)
     * @param targetPos The intended target BlockPos
     *
     */
    public static Vec3 calculateCannonAngles(CannonMountBlockEntity mount,
                                                 AbstractMountedCannonContraption cannon, BlockPos endPos, BlockPos targetPos)
    {
        try {
            PitchOrientedContraptionEntity entity = mount.getContraption();
            //AbstractMountedCannonContraption cannon = (AbstractMountedCannonContraption) entity;
            Level level = mount.getLevel();
            Direction initialOrientation = cannon.initialOrientation();
            AbstractBigCannonProjectile projectile = null;
            float totalCharges = 0;
            int barrelLength = 0;
            boolean isDropMortar = false;
            List<StructureTemplate.StructureBlockInfo> projectileBlocks = new ArrayList<>();
            BlockPos currentPos = endPos;
            while(cannon.presentBlockEntities.get(currentPos) instanceof IBigCannonBlockEntity cbe)
            {
                BigCannonBehavior behavior = cbe.cannonBehavior();
                StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();
                StructureTemplate.StructureBlockInfo cannonInfo = cannon.getBlocks().get(currentPos);
                if (cannonInfo == null) break;

                /* if(cannonInfo instanceof BigCannonBarrelBlock ) {
                    barrelLength++;
                }
                */
                Block block = containedBlockInfo.state().getBlock();

                if(block instanceof DropMortarEndBlock) {
                    isDropMortar = true;
                    break;
                }

                if (block instanceof BigCannonPropellantBlock propellant && !(block instanceof ProjectileBlock)) {
                    totalCharges += Math.max(0f, propellant.getChargePower(containedBlockInfo));
                }

                if (block instanceof ProjectileBlock<?> projBlock && projectile == null)
                {
                    projectileBlocks.add(containedBlockInfo);
                    if (projBlock.isComplete(projectileBlocks, initialOrientation)) {
                        projectile = projBlock.getProjectile(level, projectileBlocks);
                        totalCharges += projectile.addedChargePower();
                    }

                }

                currentPos = currentPos.relative(initialOrientation);
            }

            BallisticPropertiesComponent props = CBC_BIGCANNON_PROPS.getPropertiesOf(CBC_SOLID_SHOT_TYPE).ballistics();
            if(isDropMortar)
                props = CBC_BIGCANNON_PROPS.getPropertiesOf(CBC_DROP_MORTAR_SHELL_TYPE).ballistics();
            if (projectile != null) {
                /**
                 * protected BallisticPropertiesComponent getBallisticProperties() {
                 *         return this.getAllProperties().ballistics();
                 *     }
                 *     invoke getballisticProperties on projectile using reflection
                 */
                try {
                    Class<?> clazz = projectile.getClass();
                    java.lang.reflect.Method method = clazz.getDeclaredMethod("getBallisticProperties");
                    method.setAccessible(true);
                    props = (BallisticPropertiesComponent) method.invoke(projectile);

                } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

                // Calculate spawn position (muzzle position)
                Vec3 projSpawnPos = entity.toGlobalVector(
                    Vec3.atCenterOf(currentPos.relative(initialOrientation)), 0);
                Vec3 vec = projSpawnPos.subtract(
                    entity.toGlobalVector(Vec3.atCenterOf(BlockPos.ZERO), 0)).normalize();
                projSpawnPos = projSpawnPos.subtract(vec.scale(2));

            Vec3 targetAngle = Vec3.atCenterOf(targetPos).subtract(projSpawnPos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-targetAngle.x, targetAngle.z));

            //Vec from Minecraft shoot function: projectile.shoot(vec.x, vec.y, vec.z, propelCtx.chargesUsed, propelCtx.spread);
            double dist = Math.sqrt(
                Math.pow(targetPos.getX() - projSpawnPos.x, 2) + Math.pow(targetPos.getZ() - projSpawnPos.z, 2)
            );


            double v0 = Math.max( totalCharges, 1)*20; //once per tick
            double airTime = dist/v0; //in seconds
            double formDrag = props.drag();
            double density = DimensionMunitionPropertiesHandler.getProperties(level).dragMultiplier();
            //props.durabilityMass() not used in drag equation

            double gm = DimensionMunitionPropertiesHandler.getProperties(level).gravityMultiplier();
            double gravity = -1*props.gravity() * gm;//-1 factored out of equation


            //Determine the pitch we need to launch at to stay in the air long enough
            double h0 = targetPos.getY() - projSpawnPos.y;
            double requiredVy = (h0 + 0.5 * gravity * airTime * airTime) / airTime;

            //factor drag
            if(requiredVy > v0) { return null; }

            //set yaw and recalculate dist
            double pitchRads = Math.asin(requiredVy / v0);
            mount.setPitch((float) Math.toDegrees(pitchRads));
            mount.setYaw((float) yaw);
            mount.notifyUpdate();

            projSpawnPos = entity.toGlobalVector(Vec3.atCenterOf(currentPos.relative(initialOrientation)), 0);
            vec = projSpawnPos.subtract(entity.toGlobalVector(Vec3.atCenterOf(BlockPos.ZERO), 0)).normalize();
            projSpawnPos = projSpawnPos.subtract(vec.scale(2));

            h0 = targetPos.getY() - projSpawnPos.y;
            requiredVy = (h0 + 0.5 * gravity * airTime * airTime) / airTime;
            pitchRads = Math.asin(requiredVy / v0);
            mount.setPitch((float) Math.toDegrees(pitchRads));
            mount.notifyUpdate();

            final int tries = 10;
            double xDist = 0;
            for(int i=0;i<tries;i++)
            {
                //setting the pitch changes the spawn position of the projectile
                dist = Math.sqrt( Math.pow(targetPos.getX() - projSpawnPos.x, 2) + Math.pow(targetPos.getZ() - projSpawnPos.z, 2) );
                System.out.println("Try " + i + ": Target dist: " + dist);
                xDist = simulateLaunchDist(pitchRads, v0, formDrag, airTime, density, gravity, projSpawnPos.y, targetPos.getY(), i);
                //double diff = dist - xDist;
                double err = (1 - xDist/dist);
                double adjustPitch = 0;
                if(Math.abs(err) < DIST_ERROR_TABLE[0][0]) break;

                for (double[] entry : DIST_ERROR_TABLE) {
                    if (err <= entry[0]) {
                        pitchRads += (xDist < dist) ?  entry[1] : -entry[1];
                        break;
                    }
                }
                double vx = Math.cos(pitchRads) * v0;
                airTime =2*dist/vx;

            }
            double pitch = Math.toDegrees(pitchRads);


            //actual XDist traveled and highest Y reached
            LoggerProject.logInfo("020006", "Estimated total X Dist: " + xDist + " to pos "+ (projSpawnPos.x + xDist) );
            //LoggerProject.logInfo("020007", "Estimated total Y Dist: " + yDist + " to max height "+ (muzzlePos.y + yDist) );

            return new Vec3(yaw, pitch, 0);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Return total x dist and max y height as function of drag
     * @return
    */
    private static int MAX_ITERS = 10000;
    public static Double simulateLaunchDist(double pitchRads, double v0, double formDrag, double airTime, double density, double gravity,
    double startPos, double targetPos, int attempt )
    {
        double dt = 0.05; //1/20 sec per tick
        double vty = Math.sin(pitchRads)*v0*dt;
        double vtx = Math.cos(pitchRads)*v0*dt;
        double xDist = 0;
        double yDist = 0;

        double speed = Math.sqrt(vtx * vtx + vty * vty);
        double dragMagnitude = formDrag * density * speed;
        dragMagnitude = Math.min(dragMagnitude, speed);
        double accX = -(vtx / speed) * dragMagnitude;
        double accY = -(vty / speed) * dragMagnitude - (gravity);
         for(int t=0;t<airTime*20;t++)
        //for(int t=0;t<MAX_ITERS;t++)
         {
            xDist += vtx;
            yDist += vty;
            if(attempt==0 || attempt>6 )
            System.out.printf("Tick: pos=(%.6f, %.6f, %.6f) velocity=(%.6f, %.6f, %.6f)%n",
                xDist, yDist, 0f, vtx, vty, 0f);

            vtx += accX*V_FACTOR;
            vty += accY*V_FACTOR;

            if( (vty<0) && (startPos + yDist < targetPos-1)) {
                break;
            }
         speed = Math.sqrt(vtx * vtx + vty * vty);
         dragMagnitude = formDrag * density * speed;
         dragMagnitude = Math.min(dragMagnitude, speed);
         accX = -(vtx / speed) * dragMagnitude;
         accY = -(vty / speed) * dragMagnitude - (gravity);
        }

        return xDist;
    }

    /*
    public static Double simulateLaunchDist(double pitchRads, double v0, double formDrag,
                                            double airTime, double density, double gravity,
                                            double startPos, double targetPos) {
        double dt = 0.05; // seconds per tick

        // Convert v0 from blocks/second to blocks/tick
        double vty = Math.sin(pitchRads) * v0 * dt;
        double vtx = Math.cos(pitchRads) * v0 * dt;

        double xDist = 0;
        double yDist = 0;

        for(int t = 0; t < airTime * 20; t++) {
            // Calculate total speed (blocks/tick)
            double speed = Math.sqrt(vtx * vtx + vty * vty);

            if(speed < 0.0001) break; // Stopped moving

            // Calculate drag magnitude based on TOTAL speed
            double dragMagnitude = formDrag * density * speed;
            // For quadratic drag: dragMagnitude *= speed;
            dragMagnitude = Math.min(dragMagnitude, speed);

            // Decompose drag into components (opposes velocity direction)
            double dragAccelX = -(vtx / speed) * dragMagnitude;
            double dragAccelY = -(vty / speed) * dragMagnitude;

            // Total acceleration (blocks/tick²)
            double accelX = dragAccelX;
            double accelY = dragAccelY - (gravity * dt);

            // Semi-implicit Euler: position update with old velocity + half acceleration
            xDist += vtx + accelX * 0.5;
            yDist += vty + accelY * 0.5;

            // Update velocities with full acceleration
            vtx += accelX;
            vty += accelY;

            // Check if projectile has hit ground
            if(vty < 0 && (startPos + yDist) <= targetPos) {
                break;
            }
        }

        return xDist;
    }
    */


    /**
     * Converts pitch and yaw angles to a direction vector.
     *
     * @param pitch The pitch angle in degrees (negative = down, positive = up)
     * @param yaw The yaw angle in degrees (0/360 = North, 90 = East, 180 = South, 270 = West)
     * @return The normalized direction vector
     */
    public static Vec3 getDirectionFromAngles(float pitch, float yaw) {
        // Convert degrees to radians
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        // Calculate direction vector components
        // Minecraft's coordinate system:
        // North = -Z, South = +Z, East = +X, West = -X
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vec3(x, y, z);
    }

}
