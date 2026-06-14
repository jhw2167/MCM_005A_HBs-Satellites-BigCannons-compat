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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannon_control.fixed_cannon_mount.FixedCannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannons.ICannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.AutocannonBaseBlock;
import rbasamoyai.createbigcannons.cannons.autocannon.AutocannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.IAutocannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.breech.AutocannonBreechBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;
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
import static rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlock.ASSEMBLY_POWERED;

public class RemoteCannonWeapon implements RemoteCannonWeaponCommon {

    private Messager msgr;

    private BlockEntityType<CannonMountBlockEntity> cbcCannonMountType;
    private BlockEntityType<CannonMountBlockEntity> cbcFixedMountType;
    private Block cbcSolidShot;
    private EntityType<SolidShotProjectile> cbcSolidShotType;
    private EntityType<DropMortarShellProjectile> cbcDropMortarShellType;

    private InertAutocannonProjectilePropertiesHandler cbcAutocannonProps;
    private InertBigCannonProjectilePropertiesHandler cbcBigcannonProps;
    private FlakAutocannonProjectilePropertiesHandler flakAutocannonProps;

    // Narrow per-mount adapter; ControlPitchContraption.Block does not expose setYaw/setPitch/getContraption.
    private interface MountOps {
        void setYaw(float v);
        void setPitch(float v);
        void notifyUpdate();
        BlockPos pos();
        Level level();
        boolean isRunning();
        boolean isFixed();
    }

    @Override
    public void init(EventRegistrar reg) {
        reg.registerOnBeforeServerStarted(this::onServerStarting);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onServerStarting(ServerStartingEvent event) {
        Registry<BlockEntityType> beRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BLOCK_ENTITY_TYPE);
        Registry<Block> blockRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BLOCK);
        Registry<EntityType<?>> entityRegistry = event.getServer().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);

        this.cbcCannonMountType = beRegistry.get(CBC_CANNON_MOUNT_ID);
        this.cbcFixedMountType = beRegistry.get(CBC_FIXED_MOUNT_ID);
        this.cbcSolidShotType = (EntityType<SolidShotProjectile>) entityRegistry.get(CBC_SOLID_SHOT_TYPE_ID);
        this.cbcDropMortarShellType = (EntityType<DropMortarShellProjectile>) entityRegistry.get(CBC_DROP_MORTAR_SHELL_TYPE_ID);

        this.cbcAutocannonProps = CBCMunitionPropertiesHandlers.INERT_AUTOCANNON_PROJECTILE;
        this.cbcBigcannonProps = CBCMunitionPropertiesHandlers.INERT_BIG_CANNON_PROJECTILE;
        this.flakAutocannonProps = CBCMunitionPropertiesHandlers.FLAK_AUTOCANNON;

        // Both mount types route through the 2-arg interface methods; dispatch happens inside.
        TargetReceiverBlockEntity.addNeighborBlockEntityWeapons(this.cbcCannonMountType, this::cannonOnFire, this::cannonOnTargetSet);
        TargetReceiverBlockEntity.addNeighborBlockEntityWeapons(this.cbcFixedMountType,  this::cannonOnFire, this::cannonOnTargetSet);
        this.msgr = Messager.getInstance();
    }

    // Build a per-BE adapter that gives uniform access to mount controls; null for non-mounts.
    private static MountOps mountOps(BlockEntity be) {
        if (be instanceof CannonMountBlockEntity m) {
            return new MountOps() {
                public void setYaw(float v) { m.setYaw(v); }
                public void setPitch(float v) { m.setPitch(v); }
                public void notifyUpdate() { m.notifyUpdate(); }
                public BlockPos pos() { return m.getBlockPos(); }
                public Level level() { return m.getLevel(); }
                public boolean isRunning() { return m.isRunning(); }
                public boolean isFixed() { return false; }
            };
        }
        if (be instanceof FixedCannonMountBlockEntity m) {
            return new MountOps() {
                // Fixed mounts have no setYaw/setPitch on the BE — write directly to the contraption.
                public void setYaw(float v) {
                    PitchOrientedContraptionEntity c = m.getContraption();
                    if (c == null) return;
                    c.yaw = v;
                    c.prevYaw = v;
                    c.setYRot(v);
                    c.yRotO = c.getYRot();
                }
                public void setPitch(float v) {
                    PitchOrientedContraptionEntity c = m.getContraption();
                    if (c == null) return;
                    c.pitch = v;
                    c.prevPitch = v;
                    c.setXRot(v);
                    c.xRotO = c.getXRot();
                }
                public void notifyUpdate() { m.notifyUpdate(); }
                public BlockPos pos() { return m.getBlockPos(); }
                public Level level() { return m.getLevel(); }

                @Override
                public boolean isRunning() {return true;
                }

                public boolean isFixed() { return true; }
            };
        }
        return null;
    }

    // Resolve the pitch-oriented contraption directly since the common Block interface doesn't expose it.
    private static PitchOrientedContraptionEntity getPitchEntity(BlockEntity be) {
        if (be instanceof CannonMountBlockEntity m) return m.getContraption();
        if (be instanceof FixedCannonMountBlockEntity m) return m.getContraption();
        return null;
    }

    @Override
    public void cannonOnTargetSet(TargetReceiverBlockEntity receiver, BlockEntity blockEntity) {
        MountOps mount = mountOps(blockEntity);
        if (mount == null) return;

        PitchOrientedContraptionEntity cannonAngler = getPitchEntity(blockEntity);
        if (cannonAngler == null) return;

        if (cannonAngler.getContraption() instanceof MountedAutocannonContraption autocannon) {
            autoCannonOnTargetSet(receiver, mount, cannonAngler, autocannon);
        } else if (cannonAngler.getContraption() instanceof AbstractMountedCannonContraption bigCannon) {
            cannonOnTargetSet(receiver, mount, cannonAngler, bigCannon);
            if(blockEntity instanceof  FixedCannonMountBlockEntity fbe) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("Pitch", (int) cannonAngler.pitch);
                tag.putInt("Yaw", (int) cannonAngler.yaw);
                fbe.readFromClipboard(null, tag, null, null, false);
            }
        }
    }

    @Override
    public void cannonOnFire(TargetReceiverBlockEntity receiver, BlockEntity blockEntity) {
        PitchOrientedContraptionEntity cannonAngler = getPitchEntity(blockEntity);
        if (cannonAngler == null) return;

        if (blockEntity instanceof FixedCannonMountBlockEntity fixed) {
            fixedMountOnFire(receiver, fixed, cannonAngler);
            return;
        }
        if (!(blockEntity instanceof CannonMountBlockEntity be)) return;
        if (!be.isRunning()) return;

        BlockState state = be.getBlockState();
        if (cannonAngler.getContraption() instanceof MountedAutocannonContraption autocannon) {
            autocannonOnFire(autocannon);
            return;
        }
        boolean prevAssemblyPowered = state.getValue(ASSEMBLY_POWERED);
        be.onRedstoneUpdate(prevAssemblyPowered, true, true, false, 15);
        LoggerProject.logInfo("020001", "Cannon Fire: ");
    }

    // Fixed mounts have no ASSEMBLY_POWERED state — fake a rising edge into onRedstoneUpdate.
    private void fixedMountOnFire(TargetReceiverBlockEntity receiver,
                                  FixedCannonMountBlockEntity be,
                                  PitchOrientedContraptionEntity cannonAngler) {
        if (cannonAngler.getContraption() instanceof MountedAutocannonContraption autocannon) {
            autocannonOnFire(autocannon);
            return;
        }
        be.onRedstoneUpdate(false, true, true, false, 15);
        LoggerProject.logInfo("020001", "Fixed Cannon Fire: ");
    }

    private void autoCannonOnTargetSet(TargetReceiverBlockEntity receiver,
                                       MountOps mount,
                                       PitchOrientedContraptionEntity cannonAngler,
                                       MountedAutocannonContraption autoCannon) {
        Player p = receiver.getPlayerFiredWeapon();
        BlockPos targetPos = receiver.getUiTargetBlockPos();
        BlockPos mountPos = mount.pos();
        if (targetPos == null || mountPos == null) return;

        if (BlockUtil.distanceSqr(targetPos, mountPos) < MIN_RADIUS * MIN_RADIUS) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            msgr.sendBottomActionHint(p, id + ": Not in Range!");
            return;
        }

        int dx = targetPos.getX() - mountPos.getX();
        int dz = targetPos.getZ() - mountPos.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        if (yaw < 0) yaw += 360;

        BlockPos endPos = autoCannon.getStartPos();
        Vec3 angles;
        if (autoCannon.presentBlockEntities.get(endPos) instanceof IAutocannonBlockEntity) {
            angles = calculateCannonAngles(mount, cannonAngler, autoCannon, endPos, targetPos);
        } else {
            return;
        }

        if (angles == null) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            msgr.sendBottomActionHint(p, id + ": Target out of range!");
            return;
        }

        mount.setYaw((float) angles.x);
        mount.setPitch((float) angles.y);
        cannonAngler.setYRot((float) angles.x);
        mount.notifyUpdate();
    }

    private void cannonOnTargetSet(TargetReceiverBlockEntity receiver,
                                   MountOps mount,
                                   PitchOrientedContraptionEntity cannonAngler,
                                   AbstractMountedCannonContraption cannon) {
        Player p = receiver.getPlayerFiredWeapon();
        BlockPos targetPos = receiver.getUiTargetBlockPos();
        BlockPos mountPos = mount.pos();
        if (targetPos == null || mountPos == null) return;

        if (BlockUtil.distanceSqr(targetPos, mountPos) < MIN_RADIUS * MIN_RADIUS) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            msgr.sendBottomActionHint(p, id + ": Not in Range!");
            return;
        }

        int dx = targetPos.getX() - mountPos.getX();
        int dz = targetPos.getZ() - mountPos.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        if (yaw < 0) yaw += 360;

        BlockPos endPos = cannon.getStartPos().relative(cannonAngler.getInitialOrientation().getOpposite());
        Vec3 angles;
        if (cannon.presentBlockEntities.get(endPos) instanceof IBigCannonBlockEntity) {
            angles = calculateCannonAngles(mount, cannonAngler, cannon, endPos, targetPos);
        } else {
            return;
        }

        if (angles == null) {
            String id = WEAPON_ID.replace("{pos}", BlockUtil.positionToString(mountPos));
            msgr.sendBottomActionHint(p, id + ": Target out of range!");
            return;
        }

        mount.setYaw((float) angles.x);
        mount.setPitch((float) angles.y);
        cannonAngler.setYRot((float) angles.x);
        mount.notifyUpdate();
    }

    public void autocannonOnFire(MountedAutocannonContraption autocannon) {
        if (autocannon == null) return;
        for (BlockEntity abe : autocannon.presentBlockEntities.values()) {
            if (abe instanceof AutocannonBreechBlockEntity breech) {
                int currentRate = breech.getFireRate();
                breech.setFireRate(currentRate != 0 ? 0 : 15);
                return;
            }
        }
    }

    public Vec3 calculateCannonAngles(MountOps mount,
                                      PitchOrientedContraptionEntity entity,
                                      AbstractMountedCannonContraption cannon,
                                      BlockPos endPos, BlockPos targetPos) {
        try {
            Level level = mount.level();
            Direction initialOrientation = cannon.initialOrientation();
            // Fixed cannon sits directly above the mount; normal mount has a 1-block gap.
            int muzzleClearance = mount.isFixed() ? 1 : 2;

            AbstractBigCannonProjectile projectile = null;
            float totalCharges = 0;
            int barrelLength = 0;
            boolean isDropMortar = false;
            List<StructureTemplate.StructureBlockInfo> projectileBlocks = new ArrayList<>();
            BlockPos currentPos = endPos;
            AutocannonMaterial material = null;

            while (cannon.presentBlockEntities.get(currentPos) instanceof ICannonBlockEntity cbe) {
                if (cbe instanceof AutocannonBlockEntity acbe) {
                    AutocannonBaseBlock block = (AutocannonBaseBlock) acbe.getBlockState().getBlock();
                    material = block.getAutocannonMaterial();
                    barrelLength++;
                }
                if (cbe instanceof IBigCannonBlockEntity bcbe) {
                    BigCannonBehavior behavior = bcbe.cannonBehavior();
                    StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();
                    StructureTemplate.StructureBlockInfo cannonInfo = cannon.getBlocks().get(currentPos);
                    if (cannonInfo == null) break;
                    Block block = containedBlockInfo.state().getBlock();

                    if (cannonInfo.state().getBlock() instanceof DropMortarEndBlock) {
                        isDropMortar = true;
                        break;
                    }

                    if (block instanceof BigCannonPropellantBlock propellant && !(block instanceof ProjectileBlock)) {
                        totalCharges += Math.max(0f, propellant.getChargePower(containedBlockInfo));
                    }
                    if (block instanceof ProjectileBlock<?> projBlock && projectile == null) {
                        projectileBlocks.add(containedBlockInfo);
                        if (projBlock.isComplete(projectileBlocks, initialOrientation)) {
                            projectile = projBlock.getProjectile(level, projectileBlocks);
                            totalCharges += projectile.addedChargePower();
                        }
                    }
                }
                currentPos = currentPos.relative(initialOrientation);
            }

            BallisticPropertiesComponent props = this.cbcBigcannonProps.getPropertiesOf(this.cbcSolidShotType).ballistics();
            if (isDropMortar) {
                props = this.cbcBigcannonProps.getPropertiesOf(this.cbcDropMortarShellType).ballistics();
            }

            if (material != null) {
                totalCharges = material.properties().baseSpeed();
                for (int i = 0; i < barrelLength; i++) {
                    if (barrelLength > material.properties().maxBarrelLength()) break;
                    totalCharges += material.properties().speedIncreasePerBarrel();
                }
                props = new BallisticPropertiesComponent(0.05f, 0.01f, false, 0f, 0f, 0f, 0f);
            }

            if (projectile != null) {
                try {
                    Class<?> clazz = projectile.getClass();
                    java.lang.reflect.Method method = clazz.getDeclaredMethod("getBallisticProperties");
                    method.setAccessible(true);
                    props = (BallisticPropertiesComponent) method.invoke(projectile);
                } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            Vec3 projSpawnPos = entity.toGlobalVector(Vec3.atCenterOf(currentPos.relative(initialOrientation)), 0);
            Vec3 vec = projSpawnPos.subtract(entity.toGlobalVector(Vec3.atCenterOf(BlockPos.ZERO), 0)).normalize();
            projSpawnPos = projSpawnPos.subtract(vec.scale(muzzleClearance));

            Vec3 targetAngle = Vec3.atCenterOf(targetPos).subtract(projSpawnPos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-targetAngle.x, targetAngle.z));

            double dist = Math.sqrt(
                Math.pow(targetPos.getX() - projSpawnPos.x, 2) + Math.pow(targetPos.getZ() - projSpawnPos.z, 2)
            );

            double v0 = Math.max(totalCharges, 1) * 20;
            double airTime = dist / v0;
            double formDrag = props.drag();
            double density = DimensionMunitionPropertiesHandler.getProperties(level).dragMultiplier();
            double gm = DimensionMunitionPropertiesHandler.getProperties(level).gravityMultiplier();
            double gravity = -1 * props.gravity() * gm;

            double h0 = targetPos.getY() - projSpawnPos.y;
            double requiredVy = (h0 + 0.5 * gravity * airTime * airTime) / airTime;
            if (requiredVy > v0) return null;

            double pitchRads = Math.asin(requiredVy / v0);
            mount.setPitch((float) Math.toDegrees(pitchRads));
            mount.setYaw((float) yaw);
            mount.notifyUpdate();

            projSpawnPos = entity.toGlobalVector(Vec3.atCenterOf(currentPos.relative(initialOrientation)), 0);
            vec = projSpawnPos.subtract(entity.toGlobalVector(Vec3.atCenterOf(BlockPos.ZERO), 0)).normalize();
            projSpawnPos = projSpawnPos.subtract(vec.scale(muzzleClearance));

            h0 = targetPos.getY() - projSpawnPos.y;
            requiredVy = (h0 + 0.5 * gravity * airTime * airTime) / airTime;
            pitchRads = Math.asin(requiredVy / v0);
            mount.setPitch((float) Math.toDegrees(pitchRads));
            mount.notifyUpdate();

            final int tries = 10;
            double xDist = 0;
            for (int i = 0; i < tries; i++) {
                dist = Math.sqrt(Math.pow(targetPos.getX() - projSpawnPos.x, 2) + Math.pow(targetPos.getZ() - projSpawnPos.z, 2));
                xDist = simulateLaunchDist(pitchRads, v0, formDrag, airTime, density, gravity, projSpawnPos.y, targetPos.getY(), i);
                double err = (1 - xDist / dist);
                if (Math.abs(err) < DIST_ERROR_TABLE[0][0]) break;

                for (double[] entry : DIST_ERROR_TABLE) {
                    if (err <= entry[0]) {
                        pitchRads += (xDist < dist) ? entry[1] : -entry[1];
                        break;
                    }
                }
                double vx = Math.cos(pitchRads) * v0;
                airTime = 2 * dist / vx;
            }
            double pitch = Math.toDegrees(pitchRads);
            return new Vec3(yaw, pitch, 0);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
