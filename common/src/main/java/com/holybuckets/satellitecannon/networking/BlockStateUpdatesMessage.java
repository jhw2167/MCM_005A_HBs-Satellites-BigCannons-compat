package com.holybuckets.satellitecannon.networking;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.satellitecannon.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class BlockStateUpdatesMessage implements CustomPacketPayload {

    public static final String LOCATION = "block_state_updates";
    private static final Integer BLOCKPOS_SIZE = 48;

    public static final CustomPacketPayload.Type<BlockStateUpdatesMessage> TYPE =
        new CustomPacketPayload.Type<>(HBUtil.LOC(Constants.MOD_ID, LOCATION));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockStateUpdatesMessage> STREAM_CODEC =
        CustomPacketPayload.codec(Codecs::encode, Codecs::decode);

    LevelAccessor world;
    Map<BlockState, List<BlockPos>> blockStates;

    BlockStateUpdatesMessage(LevelAccessor level, Map<BlockState, List<BlockPos>> blocks) {
        this.world = level;
        this.blockStates = blocks;
    }

    public static void createAndFire(LevelAccessor world, Map<BlockState, List<BlockPos>> updates) {
        BlockStateUpdatesMessageHandler.createAndFire(world, updates);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
