package com.holybuckets.foundation.event;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.custom.*;
import com.holybuckets.foundation.event.custom.DatastoreSaveEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import com.holybuckets.foundation.networking.ClientInputMessage;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.util.MixinManager;
import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.BreakBlockEvent;
import net.blay09.mods.balm.api.event.PlayerAttackEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import javax.annotation.Nullable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.holybuckets.foundation.event.custom.ServerTickEvent.DailyTickEvent;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class EventRegistrar {
    public static final String CLASS_ID = "010";

    /**
     * World Data
     **/
    private static EventRegistrar instance;
    final Map<Integer, EventPriority> PRIORITIES = new HashMap<>();

    final Set<Consumer<PlayerLoginEvent>> ON_PLAYER_LOGIN = new ConcurrentSet<>();
    final Set<Consumer<PlayerLogoutEvent>> ON_PLAYER_LOGOUT = new ConcurrentSet<>();
    final Set<Consumer<LevelLoadingEvent.Load>> ON_LEVEL_LOAD = new ConcurrentSet<>();
    final Set<Consumer<LevelLoadingEvent.Unload>> ON_LEVEL_UNLOAD = new ConcurrentSet<>();


    final Set<Consumer<ChunkLoadingEvent.Load>> ON_CHUNK_LOAD = new ConcurrentSet<>();
    final Set<Consumer<ChunkLoadingEvent.Unload>> ON_CHUNK_UNLOAD = new ConcurrentSet<>();

    //final Deque<Consumer<ModLifecycleEvent>> ON_MOD_LIFECYCLE = new ArrayDeque<>();
    //Will have to divide up into different lifecycles

    //final Deque<Consumer<RegisterEvent>> ON_REGISTER = new ArrayDeque<>();
    //Dont see it, I think this is for registering commands

    //final Deque<Consumer<ModConfigEvent>> ON_MOD_CONFIG = new ArrayDeque<>();
    //Dont see it, is probably different for forge and fabric, Balm abstracts away all configuration

    final Set<Consumer<ServerStartingEvent>> ON_BEFORE_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStartedEvent>> ON_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStoppedEvent>> ON_SERVER_STOP = new ConcurrentSet<>();

    final Map<TickScheme, Consumer<?>> SERVER_TICK_EVENTS = new ConcurrentHashMap<>();
    final Multimap<ResourceLocation, Consumer<DailyTickEvent>> DAILY_TICK_EVENTS = HashMultimap.create();
    final Set<Consumer<DatastoreSaveEvent>> ON_DATA_SAVE = new ConcurrentSet<>();
    final Set<Consumer<PlayerAttackEvent>> ON_PLAYER_ATTACK = new ConcurrentSet<>();
    final Set<Consumer<BreakBlockEvent>> ON_BLOCK_BROKEN = new ConcurrentSet<>();
    final Set<Consumer<PlayerChangedDimensionEvent>> ON_PLAYER_CHANGED_DIMENSION = new ConcurrentSet<>();
    final Set<Consumer<PlayerRespawnEvent>> ON_PLAYER_RESPAWN = new ConcurrentSet<>();
    final Set<Consumer<LivingDeathEvent>> ON_PLAYER_DEATH = new ConcurrentSet<>();
    final Set<Consumer<UseBlockEvent>> ON_USE_BLOCK = new ConcurrentSet<>();
    final Set<Consumer<PlayerAttackEvent>> ON_PLAYER_ATTACK_EVENT = new ConcurrentSet<>();
    final Set<Consumer<DigSpeedEvent>> ON_DIG_SPEED_EVENT = new ConcurrentSet<>();
    final Set<Consumer<ClientInputEvent>> ON_CLIENT_INPUT = new ConcurrentSet<>();
    final Set<Consumer<WakeUpAllPlayersEvent>> ON_WAKE_UP_ALL_PLAYERS = new ConcurrentSet<>();
    final Set<Consumer<TossItemEvent>> ON_TOSS_ITEM = new ConcurrentSet<>();
    final Multimap<String, Consumer<SimpleMessageEvent>> ON_SIMPLE_MESSAGE = HashMultimap.create();
    final Set<Consumer<StructureLoadedEvent>> ON_STRUCTURE_LOADED = new ConcurrentSet<>();
    final Map<ResourceLocation, Set<Consumer<PlayerNearStructureEvent>>> ON_PLAYER_NEAR_STRUCTURE = new ConcurrentHashMap<>();

    // Cache for event ID strings using HashBasedTable with consumer and event class as separate indices
    private final Table<Integer, Class<?>, String> eventIdCache = HashBasedTable.create();

    /**
     * Constructor
     **/
    private EventRegistrar() {
        super();
        LoggerBase.logInit(null, "010000", this.getClass().getName());

        instance = this;
    }

    public static EventRegistrar getInstance() {
        return instance;
    }

    public static void init() {
        instance = new EventRegistrar();
        BalmEventRegister.registerPriorityEvents(instance);
    }

    void onBeforeServerStarted(ServerStartingEvent event) {
        // Sort consumers by priority
        List<Consumer<ServerStartingEvent>> sortedConsumers = ON_BEFORE_SERVER_START.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();

        GeneralConfig.fireEvent(ServerStartingEvent.class, event);
        // Execute in priority order
        for (Consumer<ServerStartingEvent> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }

    }

    void onServerStopped(ServerStoppedEvent event) {
        // Sort consumers by priority
        List<Consumer<ServerStoppedEvent>> sortedConsumers = ON_SERVER_STOP.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();
            
        // Execute in priority order
        for (Consumer<ServerStoppedEvent> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }

        GeneralConfig.fireEvent(ServerStoppedEvent.class, event);
    }

    void onServerStarted(ServerStartedEvent event) {
        // Sort consumers by priority
        List<Consumer<ServerStartedEvent>> sortedConsumers = ON_SERVER_START.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();
            
        // Execute in priority order
        for (Consumer<ServerStartedEvent> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }
    }

    void onLevelLoad(LevelLoadingEvent.Load event) {
        // Sort consumers by priority
        List<Consumer<LevelLoadingEvent.Load>> sortedConsumers = ON_LEVEL_LOAD.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();

        GeneralConfig.fireEvent(LevelLoadingEvent.Load.class, event);
        // Execute in priority order
        for (Consumer<LevelLoadingEvent.Load> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }
    }

    void onLevelUnload(LevelLoadingEvent.Unload event) {
        // Sort consumers by priority
        List<Consumer<LevelLoadingEvent.Unload>> sortedConsumers = ON_LEVEL_UNLOAD.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();

        GeneralConfig.fireEvent(LevelLoadingEvent.Unload.class, event);
        // Execute in priority order
        for (Consumer<LevelLoadingEvent.Unload> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }
    }


    //Create public methods for pushing functions onto each function event
    private <T> void generalRegister(Consumer<T> function, Set<Consumer<T>> set, EventPriority priority) {
        set.add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    private void generalTickEventRegister(Consumer<?> function, Map<TickScheme, Consumer<?>> map, TickType type, EventPriority priority) {
        TickScheme scheme = new TickScheme(function, type);
        map.put(scheme, function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnPlayerLogin(Consumer<PlayerLoginEvent> function) {
        registerOnPlayerLogin(function, EventPriority.Normal);
    }

    public void registerOnPlayerLogin(Consumer<PlayerLoginEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOGIN, priority);
    }

    public void registerOnPlayerLogout(Consumer<PlayerLogoutEvent> function) {
        registerOnPlayerLogout(function, EventPriority.Normal);
    }

    public void registerOnPlayerLogout(Consumer<PlayerLogoutEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_LOGOUT, priority);
    }


    public void registerOnLevelLoad(Consumer<LevelLoadingEvent.Load> function) {
        registerOnLevelLoad(function, EventPriority.Normal);
    }

    public void registerOnLevelLoad(Consumer<LevelLoadingEvent.Load> function, EventPriority priority) {
        generalRegister(function, ON_LEVEL_LOAD, priority);
    }


    public void registerOnLevelUnload(Consumer<LevelLoadingEvent.Unload> function) {
        registerOnLevelUnload(function, EventPriority.Normal);
    }

    public void registerOnLevelUnload(Consumer<LevelLoadingEvent.Unload> function, EventPriority priority) {
        generalRegister(function, ON_LEVEL_UNLOAD, priority);
    }


    public void registerOnChunkLoad(Consumer<ChunkLoadingEvent.Load> function) {
        registerOnChunkLoad(function, EventPriority.Normal);
    }

    public void registerOnChunkLoad(Consumer<ChunkLoadingEvent.Load> function, EventPriority priority) {
        generalRegister(function, ON_CHUNK_LOAD, priority);
    }

    public void registerOnChunkUnload(Consumer<ChunkLoadingEvent.Unload> function) {
        registerOnChunkUnload(function, EventPriority.Normal);
    }

    public void registerOnChunkUnload(Consumer<ChunkLoadingEvent.Unload> function, EventPriority priority) {
        generalRegister(function, ON_CHUNK_UNLOAD, priority);
    }

    /*
    public void registerOnModLifecycle(Consumer<ModLifecycleEvent> function) { registerOnModLifecycle(function, false); }
    public void registerOnModLifecycle(Consumer<ModLifecycleEvent> function, EventPriority priority) {
        generalRegister(function, ON_MOD_LIFECYCLE, priority);
    }

    public void registerOnRegister(Consumer<RegisterEvent> function) { registerOnRegister(function, false); }
    public void registerOnRegister(Consumer<RegisterEvent> function, EventPriority priority) {
        generalRegister(function, ON_REGISTER, priority);
    }

    public void registerOnModConfig(Consumer<ModConfigEvent> function) { registerOnModConfig(function, false); }
    public void registerOnModConfig(Consumer<ModConfigEvent> function, EventPriority priority) {
        generalRegister(function, ON_MOD_CONFIG, priority);
    }
    */


    public void registerOnBeforeServerStarted(Consumer<ServerStartingEvent> function) {
        registerOnBeforeServerStarted(function, EventPriority.Normal);
    }

    public void registerOnBeforeServerStarted(Consumer<ServerStartingEvent> function, EventPriority priority) {
        generalRegister(function, ON_BEFORE_SERVER_START, priority);
    }


    public void registerOnServerStarted(Consumer<ServerStartedEvent> function) {
        registerOnServerStarted(function, EventPriority.Normal);
    }

    public void registerOnServerStarted(Consumer<ServerStartedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_START, priority);
    }

    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function) {
        registerOnServerStopped(function, EventPriority.Normal);
    }

    public void registerOnServerStopped(Consumer<ServerStoppedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_STOP, priority);
    }



    public void registerOnDataSave(Consumer<DatastoreSaveEvent> function) {
        registerOnDataSave(function, EventPriority.Normal);
    }

    public void registerOnDataSave(Consumer<DatastoreSaveEvent> function, EventPriority priority) {
        generalRegister(function, ON_DATA_SAVE, priority);
    }


    //** TICK EVENTS

    @SuppressWarnings("unchecked")
    public <T extends ServerTickEvent> void registerOnServerTick(TickType type, Consumer<T> function) {
        registerOnServerTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ServerTickEvent> void registerOnServerTick(TickType type, Consumer<T> function, EventPriority priority) {
        generalTickEventRegister(function, SERVER_TICK_EVENTS, type, priority);
    }

    public void registerOnDailyTick(ResourceLocation dimension, Consumer<DailyTickEvent> function) {
        registerOnDailyTick(dimension, function, EventPriority.Normal);
    }

    private static final ResourceLocation EMPTY_LOC = new ResourceLocation("minecraft", "");
    /**
     * registers a consumer to a specific dimension for day changes.
     * This event is triggered when the number of ticks in a day have passed
     * OR when the player wakes up in the specifie dimension.
     * @param dimension
     * @param function
     * @param priority
     */
    public void registerOnDailyTick(@Nullable ResourceLocation dimension, Consumer<DailyTickEvent> function, EventPriority priority) {
        ResourceLocation dimLoc = dimension != null ? dimension :EMPTY_LOC;
        DAILY_TICK_EVENTS.put(dimLoc, function);
        PRIORITIES.put(function.hashCode(), priority);
    }




    //** PLAYER EVENTS

    public void registerOnPlayerAttack(Consumer<PlayerAttackEvent> function) {
        registerOnPlayerAttack(function, EventPriority.Normal);
    }

    public void registerOnPlayerAttack(Consumer<PlayerAttackEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_ATTACK, priority);
    }

    public void registerOnBreakBlock(Consumer<BreakBlockEvent> function) {
        registerOnBreakBlock(function, EventPriority.Normal);
    }

    public void registerOnBreakBlock(Consumer<BreakBlockEvent> function, EventPriority priority) {
        generalRegister(function, ON_BLOCK_BROKEN, priority);
    }

    public void registerOnPlayerChangedDimension(Consumer<PlayerChangedDimensionEvent> function) {
        registerOnPlayerChangedDimension(function, EventPriority.Normal);
    }

    public void registerOnPlayerChangedDimension(Consumer<PlayerChangedDimensionEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_CHANGED_DIMENSION, priority);
    }

    public void registerOnPlayerRespawn(Consumer<PlayerRespawnEvent> function) {
        registerOnPlayerRespawn(function, EventPriority.Normal);
    }

    public void registerOnPlayerRespawn(Consumer<PlayerRespawnEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_RESPAWN, priority);
    }

    public void registerOnPlayerDeath(Consumer<LivingDeathEvent> function) {
        registerOnPlayerDeath(function, EventPriority.Normal);
    }

    public void registerOnPlayerDeath(Consumer<LivingDeathEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_DEATH, priority);
    }

    public void registerOnUseBlock(Consumer<UseBlockEvent> function) {
        registerOnUseBlock(function, EventPriority.Normal);
    }

    public void registerOnUseBlock(Consumer<UseBlockEvent> function, EventPriority priority) {
        generalRegister(function, ON_USE_BLOCK, priority);
    }

    public void registerOnPlayerAttackEvent(Consumer<PlayerAttackEvent> function) {
        registerOnPlayerAttackEvent(function, EventPriority.Normal);
    }

    public void registerOnPlayerAttackEvent(Consumer<PlayerAttackEvent> function, EventPriority priority) {
        generalRegister(function, ON_PLAYER_ATTACK_EVENT, priority);
    }

    public void registerOnDigSpeedEvent(Consumer<DigSpeedEvent> function) {
        registerOnDigSpeedEvent(function, EventPriority.Normal);
    }

    public void registerOnDigSpeedEvent(Consumer<DigSpeedEvent> function, EventPriority priority) {
        generalRegister(function, ON_DIG_SPEED_EVENT, priority);
    }

    public void registerOnClientInput(Consumer<ClientInputEvent> function) {
        registerOnClientInput(function, EventPriority.Normal);
    }

    public void registerOnClientInput(Consumer<ClientInputEvent> function, EventPriority priority) {
        generalRegister(function, ON_CLIENT_INPUT, priority);
    }

    public void registerOnWakeUpAllPlayers(Consumer<WakeUpAllPlayersEvent> function) {
        registerOnWakeUpAllPlayers(function, EventPriority.Normal);
    }

    public void registerOnWakeUpAllPlayers(Consumer<WakeUpAllPlayersEvent> function, EventPriority priority) {
        generalRegister(function, ON_WAKE_UP_ALL_PLAYERS, priority);
    }

    public void registerOnTossItem(Consumer<TossItemEvent> function) {
        registerOnTossItem(function, EventPriority.Normal);
    }

    public void registerOnTossItem(Consumer<TossItemEvent> function, EventPriority priority) {
        generalRegister(function, ON_TOSS_ITEM, priority);
    }

    public void registerOnSimpleMessage(String messageId, Consumer<SimpleMessageEvent> function) {
        registerOnSimpleMessage(messageId, function, EventPriority.Normal);
    }

    public void registerOnSimpleMessage(String messageId, Consumer<SimpleMessageEvent> function, EventPriority priority) {
        ON_SIMPLE_MESSAGE.put(messageId, function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnStructureLoaded(Consumer<StructureLoadedEvent> function) {
        registerOnStructureLoaded(function, EventPriority.Normal);
    }

    public void registerOnStructureLoaded(Consumer<StructureLoadedEvent> function, EventPriority priority) {
        generalRegister(function, ON_STRUCTURE_LOADED, priority);
    }

    public void registerOnPlayerNearStructure(@Nullable ResourceLocation structureType, Consumer<PlayerNearStructureEvent> function) {
        registerOnPlayerNearStructure(structureType, function, EventPriority.Normal);
    }

    public void registerOnPlayerNearStructure(@Nullable ResourceLocation structureType, Consumer<PlayerNearStructureEvent> function, EventPriority priority) {
        ResourceLocation key = structureType != null ? structureType : EMPTY_LOC;
        ON_PLAYER_NEAR_STRUCTURE.computeIfAbsent(key, k -> new ConcurrentSet<>()).add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    /**
     * Custom Events
     **/

    public void dataSaveEvent(boolean writeOut) {
        DatastoreSaveEvent event = DatastoreSaveEvent.create();
        for (Consumer<DatastoreSaveEvent> saver : ON_DATA_SAVE) {
            saver.accept(event);
        }

        if( writeOut ) event.getDataStore().write();
    }

    public void onServerTick(MinecraftServer s) {
        GeneralConfig config = GeneralConfig.getInstance();
        long totalTicks = config.getTotalTickCount();
        ServerTickEvent event = new ServerTickEvent(totalTicks);
        
        // Handle regular tick events
        SERVER_TICK_EVENTS.forEach((scheme, consumer) -> {
            if (scheme.shouldTrigger(totalTicks)) {
                tryEvent((Consumer<ServerTickEvent>) consumer, event);
            }
        });

        // Handle daily tick events
        Map<ResourceLocation, DailyTickEvent> cache = new HashMap<>();

        for( Level l : config.getLevels().values())
        {
            if(l.isClientSide) continue;
            if (config.getNextDailyTick(l) > totalTicks) continue;

            ResourceLocation dimLoc = l.dimension().location();
            long sleepTicks = config.getTotalTickCountWithSleep(l);
            cache.put(dimLoc, new DailyTickEvent(totalTicks, sleepTicks, l, false));
        }

        //Fire General Config Daily events
        cache.forEach((dim, dailyTickEvent) -> GeneralConfig.fireEvent(ServerTickEvent.DailyTickEvent.class, dailyTickEvent));
        //Fire registered daily tick events
        DAILY_TICK_EVENTS.asMap().forEach((dimLoc, consumers) -> {
            if( dimLoc == EMPTY_LOC ) {
             cache.forEach((dim, dailyTickEvent) -> consumers.forEach(consumer -> tryEvent(consumer, dailyTickEvent)) );
             return;
            }
           if( !cache.containsKey(dimLoc) ) return;

           DailyTickEvent dailyTickEvent = cache.get(dimLoc);
           LoggerBase.logDebug(null, "010200", "Firing daily tick event for dimension: " + dimLoc);
           consumers.forEach(consumer -> tryEvent(consumer, dailyTickEvent));
        });
    }

    public void onServerLevelTick(Level level) {
        if( level == null ) return;
        ManagedChunkEvents.onWorldTickStart(level);
    }


    public void onWakeUpAllPlayers(ServerLevel level)
    {
        GeneralConfig config = GeneralConfig.getInstance();
        int totalSleeps = config.getTotalSleeps(level)+1;
        WakeUpAllPlayersEvent event = new WakeUpAllPlayersEvent(level, totalSleeps);
        GeneralConfig.fireEvent(WakeUpAllPlayersEvent.class, event);
        ON_WAKE_UP_ALL_PLAYERS.forEach(consumer -> tryEvent(consumer, event));

        // Trigger daily tick event if it is the first tick of the day
        DailyTickEvent dailyTickEvent = new DailyTickEvent(
                config.getTotalTickCount(),
                config.getTotalTickCountWithSleep(level),
                level,
                true
        );

        GeneralConfig.fireEvent(ServerTickEvent.DailyTickEvent.class, dailyTickEvent);
        DAILY_TICK_EVENTS.get(EMPTY_LOC).forEach(consumer -> tryEvent(consumer, dailyTickEvent) );
        ResourceLocation levelId = level.dimension().location();
        DAILY_TICK_EVENTS.get(levelId).forEach(consumer -> tryEvent(consumer, dailyTickEvent) );
    }

    public void onClientInput(ClientInputMessage message) {
        GeneralConfig config = GeneralConfig.getInstance();
        Player p = config.getServer().getPlayerList().getPlayer(message.playerId);
        ClientInputEvent event = new ClientInputEvent(p, message);
        ON_CLIENT_INPUT.forEach(consumer -> tryEvent(consumer, event));
    }

    public void onSimpleMessage(Player player, SimpleStringMessage message, String messageId) {
        SimpleMessageEvent event = new SimpleMessageEvent(player, message, messageId);
        Collection<Consumer<SimpleMessageEvent>> consumers = ON_SIMPLE_MESSAGE.get(messageId);
        
        // Sort consumers by priority
        List<Consumer<SimpleMessageEvent>> sortedConsumers = consumers.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();
            
        // Execute in priority order
        for (Consumer<SimpleMessageEvent> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }
    }

    public void onStructureLoaded(StructureLoadedEvent event) {
        // Sort consumers by priority
        List<Consumer<StructureLoadedEvent>> sortedConsumers = ON_STRUCTURE_LOADED.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();
            
        // Execute in priority order
        for (Consumer<StructureLoadedEvent> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }
    }

    public void onPlayerNearStructure(PlayerNearStructureEvent event) {
        ResourceLocation structureType = event.getStructureInfo().getId();
        
        // Get consumers for this specific structure type
        Set<Consumer<PlayerNearStructureEvent>> specificConsumers = ON_PLAYER_NEAR_STRUCTURE.get(structureType);
        if (specificConsumers != null) {
           specificConsumers.forEach(consumer -> tryEvent(consumer, event));
        }
        
        // Get consumers for all structure types (registered with null/empty ResourceLocation)
        Set<Consumer<PlayerNearStructureEvent>> generalConsumers = ON_PLAYER_NEAR_STRUCTURE.get(EMPTY_LOC);
        if (generalConsumers != null) {
              generalConsumers.forEach(consumer -> tryEvent(consumer, event));
        }
    }

    private <T> void tryEvent(Consumer<T> consumer, T event) {
        // Use consumer hashcode and event class as separate indices in the table
        String id = eventIdCache.get(consumer.hashCode(), event.getClass());
        if (id == null) {
            id = consumer.toString() + "::" + event.getClass().getName();
            eventIdCache.put(consumer.hashCode(), event.getClass(), id);
        }
            
        if( MixinManager.isEnabled(consumer.toString())) {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                MixinManager.recordError(id, e);
            }
        }
    }


    /**
     * ###############
     **/

    private class TickScheme {
        int offset;
        TickType frequency;

        <T> TickScheme(Consumer<T> func, TickType frequency) {
            this.frequency = frequency;
            this.offset = (func.hashCode() % getFrequency());
        }

        int getFrequency() {
            switch (frequency) {
                case ON_SINGLE_TICK:
                    return 1;
                case ON_20_TICKS:
                    return 20;
                case ON_120_TICKS:
                    return 120;
                case ON_1200_TICKS:
                    return 1200;
                case ON_6000_TICKS:
                    return 6000;
                case ON_24000_TICKS:
                    return 24000; // 1 day in ticks
                default:
                    return 1;
            }
        }

        public TickType getTickType() {
            return frequency;
        }

        boolean shouldTrigger(long totalTicks) {
            return totalTicks % getFrequency() == offset;
        }

    }
}
//END CLASS
