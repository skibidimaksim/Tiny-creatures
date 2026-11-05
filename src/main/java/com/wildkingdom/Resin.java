package com.wildkingdom;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Resin {
    private Resin() {}

    public static final String[] CAP = {"karapuzik","kozyavka","may_beetle","pretendyashka","cockroach","car_cockroach","telifon","strashila","fly","chironomid","fignya","dragonfly","ant","mantis","tarantula"};
    public static final int LIFETIME = 6000; // 5 minutes
    public static final int HARDEN = 1200;   // 1 minute in the mold

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, WildKingdom.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, WildKingdom.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WildKingdom.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WildKingdom.MODID);

    public static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("resin",
            () -> new FluidType(FluidType.Properties.create().density(1400).viscosity(5000)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE = FLUIDS.register("resin",
            () -> new BaseFlowingFluid.Source(props()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING = FLUIDS.register("resin_flowing",
            () -> new BaseFlowingFluid.Flowing(props()));

    public static final DeferredBlock<LiquidBlock> BLOCK = BLOCKS.registerBlock("resin",
            p -> new LiquidBlock(SOURCE.get(), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).replaceable().noCollision()
                    .strength(100f).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    public static final DeferredItem<Item> BUCKET = ITEMS.registerItem("resin_bucket",
            p -> new ResinBucketItem(SOURCE.get(), p), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> AMBER = ITEMS.registerSimpleItem("amber");

    public static final DeferredBlock<MoldBlock> MOLD = BLOCKS.registerBlock("amber_mold", MoldBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0f)
                    .sound(SoundType.GLASS).noOcclusion());
    public static final DeferredItem<BlockItem> MOLD_ITEM = ITEMS.registerSimpleBlockItem(MOLD);

    public static final Map<String, DeferredBlock<AmberBlock>> SPEC = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> SPEC_ITEM = new LinkedHashMap<>();

    static {
        for (String id : CAP) {
            DeferredBlock<AmberBlock> b = BLOCKS.registerBlock("amber_" + id, AmberBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5f)
                            .sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 3));
            SPEC.put(id, b);
            SPEC_ITEM.put(id, ITEMS.registerSimpleBlockItem(b));
        }
    }

    private static BaseFlowingFluid.Properties props() {
        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).block(BLOCK).bucket(BUCKET)
                .slopeFindDistance(2).levelDecreasePerBlock(3).tickRate(40);
    }

    public static void init(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        NeoForge.EVENT_BUS.addListener(Resin::playerTick);
    }

    public static int capIndex(String id) {
        for (int i = 0; i < CAP.length; i++) if (CAP[i].equals(id)) return i;
        return -1;
    }

    static CompoundTag tag(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    static void save(ItemStack s, CompoundTag t) {
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    private static void playerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide() || p.tickCount % 20 != 0) return;
        long now = p.level().getGameTime();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!(st.getItem() instanceof ResinBucketItem)) continue;
            CompoundTag t = tag(st);
            long born = t.getLongOr("born", -1L);
            if (born < 0) {
                t.putLong("born", now);
                save(st, t);
            } else if (now - born >= LIFETIME) {
                p.getInventory().setItem(i, new ItemStack(Items.BUCKET));
                ItemStack amber = new ItemStack(AMBER.get());
                if (!p.getInventory().add(amber)) p.drop(amber, false);
                if (p instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("msg.wildkingdom.hardened"), true);
            }
        }
    }

    public static class ResinBucketItem extends BucketItem {
        public ResinBucketItem(Fluid f, Item.Properties p) { super(f, p); }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (target instanceof WildMob m && capIndex(m.spec().id()) >= 0 && tag(stack).getStringOr("cap", "").isEmpty()) {
                if (!player.level().isClientSide()) {
                    CompoundTag t = tag(stack);
                    t.putString("cap", m.spec().id());
                    save(stack, t);
                    player.level().playSound(null, target.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
                    if (player instanceof ServerPlayer sp)
                        sp.displayClientMessage(Component.translatable("msg.wildkingdom.captured", Component.translatable("entity.wildkingdom." + m.spec().id())), true);
                    target.discard();
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    }

    public static class AmberBlock extends Block {
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);
        public AmberBlock(BlockBehaviour.Properties p) { super(p); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos pos, CollisionContext c) { return SHAPE; }
    }

    public static class MoldBlock extends Block {
        public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 2);
        public static final IntegerProperty WHO = IntegerProperty.create("who", 0, CAP.length);
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

        public MoldBlock(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(PHASE, 0).setValue(WHO, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PHASE, WHO); }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos pos, CollisionContext c) { return SHAPE; }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState st, Level level, BlockPos pos, Player p,
                                              InteractionHand hand, BlockHitResult hit) {
            if (st.getValue(PHASE) == 0 && stack.getItem() instanceof ResinBucketItem) {
                if (!level.isClientSide()) {
                    int who = capIndex(tag(stack).getStringOr("cap", ""));
                    if (who < 0) who = CAP.length;
                    level.setBlock(pos, st.setValue(PHASE, 1).setValue(WHO, who), 3);
                    level.scheduleTick(pos, this, HARDEN);
                    if (!p.isCreative()) p.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState st, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            if (st.getValue(PHASE) == 2) {
                if (!level.isClientSide()) {
                    int who = st.getValue(WHO);
                    ItemStack out = who < CAP.length ? new ItemStack(SPEC_ITEM.get(CAP[who]).get()) : new ItemStack(AMBER.get());
                    if (!p.getInventory().add(out)) p.drop(out, false);
                    level.setBlock(pos, st.setValue(PHASE, 0).setValue(WHO, 0), 3);
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        @Override
        protected void tick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
            if (st.getValue(PHASE) == 1) level.setBlock(pos, st.setValue(PHASE, 2), 3);
        }
    }
}
