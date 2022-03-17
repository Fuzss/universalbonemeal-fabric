package fuzs.universalbonemeal;

import com.google.common.collect.Sets;
import fuzs.puzzleslib.config.AbstractConfig;
import fuzs.puzzleslib.config.ConfigHolder;
import fuzs.puzzleslib.config.ConfigHolderImpl;
import fuzs.universalbonemeal.api.event.entity.player.BonemealCallback;
import fuzs.universalbonemeal.config.ServerConfig;
import fuzs.universalbonemeal.handler.BonemealHandler;
import fuzs.universalbonemeal.world.level.block.behavior.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalBoneMeal implements ModInitializer {
    public static final String MOD_ID = "universalbonemeal";
    public static final String MOD_NAME = "Universal Bone Meal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @SuppressWarnings("Convert2MethodRef")
    public static final ConfigHolder<AbstractConfig, ServerConfig> CONFIG = ConfigHolder.server(() -> new ServerConfig());

    public static void onConstructMod() {
        ((ConfigHolderImpl<?, ?>) CONFIG).addConfigs(MOD_ID);
        registerHandlers();
    }

    private static void registerHandlers() {
        BonemealHandler bonemealHandler = new BonemealHandler();
        BonemealCallback.EVENT.register(bonemealHandler::onBonemeal);
        CONFIG.addServerCallback(bonemealHandler::invalidate);
        // this is not triggered when initially loading data packs, but our values are invalid by default anyways, so that's fine
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((MinecraftServer server, CloseableResourceManager resourceManager, boolean success) -> {
            // since we compile tags into blocks we need to refresh whenever tags are reloaded
            bonemealHandler.invalidate();
            CoralBehavior.invalidate();
        });
    }

    public static void onCommonSetup() {
        registerBonemealBehaviors();
    }

    private static void registerBonemealBehaviors() {
        BonemealHandler.registerBehavior(Blocks.CACTUS, SimpleGrowingPlantBehavior::new, () -> CONFIG.server().allowCactus);
        BonemealHandler.registerBehavior(Blocks.SUGAR_CANE, SimpleGrowingPlantBehavior::new, () -> CONFIG.server().allowSugarCane);
        BonemealHandler.registerBehavior(Blocks.VINE, VineBehavior::new, () -> CONFIG.server().allowVines);
        BonemealHandler.registerBehavior(Blocks.NETHER_WART, NetherWartBehavior::new, () -> CONFIG.server().allowNetherWart);
        BonemealHandler.registerBehavior(Sets.newHashSet(Blocks.MELON_STEM, Blocks.PUMPKIN_STEM), FruitStemBehavior::new, () -> CONFIG.server().allowFruitStems);
        BonemealHandler.registerBehavior(Blocks.LILY_PAD, () -> new SimpleSpreadBehavior(4, 3), () -> CONFIG.server().allowLilyPad);
        BonemealHandler.registerBehavior(Blocks.DEAD_BUSH, () -> new SimpleSpreadBehavior(4, 2), () -> CONFIG.server().allowDeadBush);
        BonemealHandler.registerBehavior(BlockTags.SMALL_FLOWERS, () -> new SimpleSpreadBehavior(3, 1), () -> CONFIG.server().allowSmallFlowers);
        BonemealHandler.registerBehavior(BlockTags.CORAL_PLANTS, CoralBehavior::new, () -> CONFIG.server().allowCorals);
        BonemealHandler.registerBehavior(Blocks.CHORUS_FLOWER, ChorusFlowerBehavior::new, () -> CONFIG.server().allowChorus);
        BonemealHandler.registerBehavior(Blocks.CHORUS_PLANT, ChorusPlantBehavior::new, () -> CONFIG.server().allowChorus);
        BonemealHandler.registerBehavior(Blocks.MYCELIUM, MyceliumBehavior::new, () -> CONFIG.server().allowMycelium);
        BonemealHandler.registerBehavior(Sets.newHashSet(Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.DIRT_PATH), DirtBehavior::new, () -> CONFIG.server().allowDirt);
    }

    @Override
    public void onInitialize() {
        onConstructMod();
        onCommonSetup();
    }
}
