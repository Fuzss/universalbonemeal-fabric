package fuzs.universalbonemeal.handler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import fuzs.universalbonemeal.world.level.block.behavior.BonemealBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.compress.utils.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class BonemealHandler {
    private static final List<AbstractBehaviorData> BONE_MEAL_BEHAVIORS = Lists.newArrayList();

    private Map<Block, BonemealBehavior> blockToBehavior;

    public InteractionResult onBonemeal(Level level, BlockPos pos, BlockState block, ItemStack stack) {
        this.dissolve();
        BlockState block1 = block;
        BonemealBehavior behavior = this.blockToBehavior.get(block1.getBlock());
        if (behavior != null) {
            Level level1 = level;
            BlockPos pos1 = pos;
            if (behavior.isValidBonemealTarget(level1, pos1, block1, level1.isClientSide)) {
                if (level1 instanceof ServerLevel) {
                    if (behavior.isBonemealSuccess(level1, level1.random, pos1, block1)) {
                        behavior.performBonemeal((ServerLevel) level1, level1.random, pos1, block1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private void dissolve() {
        if (this.blockToBehavior == null) {
            HashMap<Block, BonemealBehavior> map = Maps.newHashMap();
            for (AbstractBehaviorData behavior : BONE_MEAL_BEHAVIORS) {
                if (behavior.allow()) {
                    behavior.compile(map);
                }
            }
            this.blockToBehavior = map;
        }
    }

    public void invalidate() {
        this.blockToBehavior = null;
    }

    public static void registerBehavior(Block block, Supplier<BonemealBehavior> factory, BooleanSupplier config) {
        BONE_MEAL_BEHAVIORS.add(new BlockBehaviorData(block, factory, config));
    }

    public static void registerBehavior(Set<Block> blocks, Supplier<BonemealBehavior> factory, BooleanSupplier config) {
        BONE_MEAL_BEHAVIORS.add(new MultiBlockBehaviorData(blocks, factory, config));
    }

    public static void registerBehavior(TagKey<Block> tag, Supplier<BonemealBehavior> factory, BooleanSupplier config) {
        BONE_MEAL_BEHAVIORS.add(new BlockTagBehaviorData(tag, factory, config));
    }

    private abstract static class AbstractBehaviorData {
        final BonemealBehavior behavior;
        private final BooleanSupplier config;

        public AbstractBehaviorData(Supplier<BonemealBehavior> factory, BooleanSupplier config) {
            this.behavior = factory.get();
            this.config = config;
        }

        public abstract void compile(Map<Block, BonemealBehavior> map);

        public boolean allow() {
            return this.config.getAsBoolean();
        }
    }

    private static class BlockBehaviorData extends AbstractBehaviorData {
        private final Block block;

        public BlockBehaviorData(Block block, Supplier<BonemealBehavior> factory, BooleanSupplier config) {
            super(factory, config);
            this.block = block;
        }

        @Override
        public void compile(Map<Block, BonemealBehavior> map) {
            map.put(this.block, this.behavior);
        }
    }

    private static class MultiBlockBehaviorData extends AbstractBehaviorData {
        private final Set<Block> targets;

        public MultiBlockBehaviorData(Set<Block> targets, Supplier<BonemealBehavior> factory, BooleanSupplier config) {
            super(factory, config);
            this.targets = ImmutableSet.copyOf(targets);
        }

        @Override
        public void compile(Map<Block, BonemealBehavior> map) {
            for (Block target : this.targets) {
                map.put(target, this.behavior);
            }
        }
    }

    private static class BlockTagBehaviorData extends AbstractBehaviorData {
        private final TagKey<Block> tag;

        public BlockTagBehaviorData(TagKey<Block> tag, Supplier<BonemealBehavior> factory, BooleanSupplier config) {
            super(factory, config);
            this.tag = tag;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void compile(Map<Block, BonemealBehavior> map) {
            for (Holder<Block> value : Registry.BLOCK.getTagOrEmpty(this.tag)) {
                map.putIfAbsent(value.value(), this.behavior);
            }
        }
    }
}
