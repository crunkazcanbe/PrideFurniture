package com.dogpound.furniture;

import java.util.List;
import java.util.Random;

import com.dogpound.furniture.Defs.Def;
import com.dogpound.furniture.Defs.Kind;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.init.PotionTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** One furniture piece. What it does depends on its {@link Kind}; the look is the Blockbench model. */
public class BlockFurniture extends Block {
    public enum Counter implements IStringSerializable {
        NONE, GRANITE, MARBLE, BUTCHER, QUARTZ, BLACK, CONCRETE;
        @Override public String getName() { return name().toLowerCase(); }
        public static Counter of(String s) { return valueOf(s.toUpperCase()); }
    }

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyEnum<Counter> COUNTER = PropertyEnum.create("counter", Counter.class);
    public static final PropertyBool ON = PropertyBool.create("on");

    public final Def def;
    private final AxisAlignedBB[] boxes = new AxisAlignedBB[4];

    public BlockFurniture(Def def) {
        super(def.kind == Kind.STORAGE && def.id.contains("oak") || def.id.contains("walnut") ? Material.WOOD : Material.IRON);
        this.def = def;
        setRegistryName(PrideFurniture.ID, def.id);
        setUnlocalizedName(PrideFurniture.ID + "." + def.id);
        setCreativeTab(PrideFurniture.TAB);
        setHardness(1.5F).setResistance(6F);
        setSoundType(getMaterial(getDefaultState()) == Material.WOOD ? SoundType.WOOD : SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(COUNTER, Counter.NONE).withProperty(ON, false));
        double[] b = def.box;
        for (EnumFacing f : EnumFacing.HORIZONTALS) boxes[f.getHorizontalIndex()] = rotate(b[0], b[1], b[2], b[3], b[4], b[5], f);
    }

    /** rotates a north-facing box to face f (models face north, blockstates turn them by y) */
    static AxisAlignedBB rotate(double x0, double y0, double z0, double x1, double y1, double z1, EnumFacing f) {
        switch (f) {
            case EAST:  return new AxisAlignedBB(1 - z1, y0, x0, 1 - z0, y1, x1);
            case SOUTH: return new AxisAlignedBB(1 - x1, y0, 1 - z1, 1 - x0, y1, 1 - z0);
            case WEST:  return new AxisAlignedBB(z0, y0, 1 - x1, z1, y1, 1 - x0);
            default:    return new AxisAlignedBB(x0, y0, z0, x1, y1, z1);
        }
    }

    // ------------------------------------------------------------------ state
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, COUNTER, ON); }
    @Override public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)).withProperty(ON, (meta & 4) != 0);
    }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex() | (s.getValue(ON) ? 4 : 0); }

    /** the countertop lives in the tile entity (7 kinds × 4 facings × on doesn't fit in 16 metas) */
    @Override public IBlockState getActualState(IBlockState s, IBlockAccess w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        return te instanceof TileFurniture ? s.withProperty(COUNTER, ((TileFurniture) te).counter) : s;
    }

    @Override public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing side, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
        if (def.kind == Kind.STAIRS) {                 // stacked spiral stairs keep turning: half a turn per block
            IBlockState below = w.getBlockState(pos.down());
            if (below.getBlock() instanceof BlockFurniture && ((BlockFurniture) below.getBlock()).def.kind == Kind.STAIRS)
                return getDefaultState().withProperty(FACING, below.getValue(FACING).getOpposite());
            IBlockState above = w.getBlockState(pos.up());
            if (above.getBlock() instanceof BlockFurniture && ((BlockFurniture) above.getBlock()).def.kind == Kind.STAIRS)
                return getDefaultState().withProperty(FACING, above.getValue(FACING).getOpposite());
        }
        boolean on = def.kind == Kind.LAMP || def.kind == Kind.FIREPLACE || def.kind == Kind.SCREEN;
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(ON, on);
    }

    @Override public float getEnchantPowerBonus(World w, BlockPos pos) { return def.kind == Kind.BOOKS ? 1 : 0; }

    /** spiral stair treads (north-facing, in pixels): N, NE, E, SE, rising 4px each, plus the centre pole */
    private static final double[][] TREADS = {{4.3, 0, 0.2, 11.7, 1, 8}, {8, 4, 0.5, 15, 5, 8}, {8, 8, 4.3, 15.8, 9, 11.7}, {8, 12, 8, 15, 13, 15}, {7, 0, 7, 9, 16, 9}};

    @Override public void addCollisionBoxToList(IBlockState s, World w, BlockPos pos, AxisAlignedBB mask, List<AxisAlignedBB> out, @javax.annotation.Nullable Entity e, boolean actual) {
        if (def.kind != Kind.STAIRS) { super.addCollisionBoxToList(s, w, pos, mask, out, e, actual); return; }
        EnumFacing f = s.getValue(FACING);
        for (double[] t : TREADS) addCollisionBoxToList(pos, mask, out, rotate(t[0] / 16, t[1] / 16, t[2] / 16, t[3] / 16, t[4] / 16, t[5] / 16, f));
    }

    // ------------------------------------------------------------------ shape + rendering
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) {
        return f == EnumFacing.UP && (def.has('c') || def.kind == Kind.STORAGE) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }
    @Override public BlockRenderLayer getBlockLayer() { return def.has('g') ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) {
        AxisAlignedBB b = boxes[s.getValue(FACING).getHorizontalIndex()];
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileFurniture && ((TileFurniture) te).counter != Counter.NONE) b = new AxisAlignedBB(b.minX, b.minY, Math.min(b.minZ, 0), b.maxX, 1, Math.max(b.maxZ, 1)).intersect(Block.FULL_BLOCK_AABB);
        return b;
    }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) {
        return def.has('n') ? NULL_AABB : getBoundingBox(s, w, p);
    }
    @Override public int getLightValue(IBlockState s, IBlockAccess w, BlockPos p) {
        if (!s.getValue(ON)) return 0;
        switch (def.kind) {
            case HOOD: return 13;
            case LAMP: case FIREPLACE: return 15;
            case SCREEN: return 6;
            default: return 0;
        }
    }

    // ------------------------------------------------------------------ tile entity
    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return TileMachine.ticks(def.kind) ? new TileMachine() : new TileFurniture(); }

    // ------------------------------------------------------------------ 2-tall / 2-long helpers
    /** where the extra block of a 2-tall (up) or 2-long (behind) piece goes */
    BlockPos extraPos(BlockPos pos, IBlockState s) {
        if (def.has('t')) return pos.up();
        if (def.has('l')) return pos.offset(s.getValue(FACING).getOpposite());
        return null;
    }

    @Override public boolean canPlaceBlockAt(World w, BlockPos pos) {
        if (!super.canPlaceBlockAt(w, pos)) return false;
        if (def.has('t')) return w.getBlockState(pos.up()).getBlock().isReplaceable(w, pos.up());
        return true;
    }

    @Override public void onBlockPlacedBy(World w, BlockPos pos, IBlockState s, EntityLivingBase placer, ItemStack stack) {
        BlockPos x = extraPos(pos, s);
        if (x == null) return;
        if (!w.getBlockState(x).getBlock().isReplaceable(w, x)) {           // no room behind a bathtub → give it back
            w.destroyBlock(pos, !(placer instanceof EntityPlayer && ((EntityPlayer) placer).capabilities.isCreativeMode));
            return;
        }
        EnumFacing toMain = def.has('t') ? EnumFacing.DOWN : s.getValue(FACING);
        w.setBlockState(x, PrideFurniture.FILLER.getDefaultState().withProperty(BlockFiller.TO_MAIN, toMain));
    }

    @Override public void breakBlock(World w, BlockPos pos, IBlockState s) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TileFurniture) {
            TileFurniture t = (TileFurniture) te;
            for (int i = 0; i < t.inv.getSlots(); i++) InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), t.inv.getStackInSlot(i));
            if (t.counter != Counter.NONE) InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(PrideFurniture.ITEMS.get("countertop_" + t.counter.getName())));
        }
        BlockPos x = extraPos(pos, s);
        if (x != null && w.getBlockState(x).getBlock() == PrideFurniture.FILLER) w.setBlockToAir(x);
        for (EntitySeat seat : w.getEntitiesWithinAABB(EntitySeat.class, new AxisAlignedBB(pos).grow(1))) seat.setDead();
        super.breakBlock(w, pos, s);
    }

    // ------------------------------------------------------------------ using it
    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing side, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        ItemStack held = p.getHeldItem(hand);
        TileEntity te = w.getTileEntity(pos);
        TileFurniture t = te instanceof TileFurniture ? (TileFurniture) te : null;
        if (held.getItem() instanceof ItemCountertop) return false;               // the countertop item handles itself
        if (def.kind == Kind.WORKBENCH) {
            if (w.isRemote) com.dogpound.furniture.client.ClientSetup.openWorkbench();
            w.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.6F, 1.2F);
            return true;
        }
        if (w.isRemote) return true;

        // take a countertop back off: sneak + empty hand
        if (t != null && p.isSneaking() && held.isEmpty() && t.counter != Counter.NONE) {
            ItemStack top = new ItemStack(PrideFurniture.ITEMS.get("countertop_" + t.counter.getName()));
            t.setCounter(Counter.NONE);
            if (!p.addItemStackToInventory(top)) p.dropItem(top, false);
            w.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1, 1);
            return true;
        }

        switch (def.kind) {
            case SINK:
            case VANITY:
                if (sink(w, pos, p, held)) return true;
                if (def.kind == Kind.VANITY || p.isSneaking()) { open(w, pos, p); return true; }
                wash(w, pos, p, 10, "§bYou wash your hands. ✨");
                return true;
            case FRIDGE:
                if (def.id.equals("fridge_smart") && p.isSneaking() && t != null) { t.screen++; t.sync(); return true; }
                w.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.6F, 1.4F);
                open(w, pos, p);
                return true;
            case HOOD:
                w.setBlockState(pos, s.withProperty(ON, !s.getValue(ON)));
                w.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.5F, 1.6F);
                return true;
            case SEAT:
                EntitySeat.sit(w, pos, p, def.id.equals("bar_stool") ? 0.85 : def.id.equals("office_chair") ? 0.62 : 0.42);
                return true;
            case TOILET:
                if (p.getRidingEntity() instanceof EntitySeat && ((EntitySeat) p.getRidingEntity()).seatPos().equals(pos)) {
                    Needs.business(p, pos);
                } else {
                    EntitySeat.sit(w, pos, p, 0.62);
                    p.sendStatusMessage(new TextComponentString("§7Right-click the toilet again to go. Sneak to get up and flush."), true);
                }
                return true;
            case TUB:
                if (held.getItem() == Items.WATER_BUCKET || held.isEmpty() && !s.getValue(ON) && !p.isSneaking() && !(p.getRidingEntity() instanceof EntitySeat)) {
                    if (!s.getValue(ON)) {
                        w.setBlockState(pos, s.withProperty(ON, true));
                        w.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1, 0.9F);
                        p.sendStatusMessage(new TextComponentString("§bThe tub fills with warm water. Right-click again to get in."), true);
                        return true;
                    }
                }
                if (held.getItem() == Items.BUCKET && s.getValue(ON)) {
                    w.setBlockState(pos, s.withProperty(ON, false));
                    w.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1);
                    return true;
                }
                EntitySeat.sit(w, pos, p, 0.25);
                return true;
            case SHOWER:
                w.setBlockState(pos, s.withProperty(ON, !s.getValue(ON)));
                w.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6F, s.getValue(ON) ? 0.8F : 1.2F);
                return true;
            case MIRROR:
                Needs n = Needs.of(p);
                p.sendStatusMessage(new TextComponentString(n == null || n.hygiene > 60 ? "§dLooking good, gorgeous. 💅" : n.hygiene > 25 ? "§eCould use a shower soon…" : "§cYikes. Shower. Now. 🚿"), true);
                return true;
            case TOWEL:
                p.extinguish();
                p.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 20 * 20));
                p.sendStatusMessage(new TextComponentString("§bAll dry and cozy. 🧺"), true);
                w.playSound(null, pos, SoundEvents.BLOCK_CLOTH_STEP, SoundCategory.BLOCKS, 1, 1);
                return true;
            case SCALE:
                Needs.weighIn(p);
                w.playSound(null, pos, SoundEvents.BLOCK_NOTE_BELL, SoundCategory.BLOCKS, 0.5F, 2F);
                return true;
            case FRUIT:
                if (t != null) t.fruit(p);
                return true;
            case TRASH:
                if (!held.isEmpty()) {
                    p.setHeldItem(hand, ItemStack.EMPTY);
                    w.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.7F, 1.3F);
                    p.sendStatusMessage(new TextComponentString("§7Tossed it. 🗑️"), true);
                }
                return true;
            case SCREEN:
                if (p.isSneaking() && t != null) { t.screen++; t.sync(); }
                else w.setBlockState(pos, s.withProperty(ON, !s.getValue(ON)));
                w.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.4F, 1.8F);
                return true;
            case LAMP:
                w.setBlockState(pos, s.withProperty(ON, !s.getValue(ON)));
                w.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4F, s.getValue(ON) ? 0.6F : 0.9F);
                return true;
            case FIREPLACE:
                w.setBlockState(pos, s.withProperty(ON, !s.getValue(ON)));
                w.playSound(null, pos, s.getValue(ON) ? SoundEvents.BLOCK_FIRE_EXTINGUISH : SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.7F, 1F);
                return true;
            case CLOCK: {
                long time = (w.getWorldTime() + 6000) % 24000;          // tick 0 = 6:00
                int h = (int) (time / 1000), m = (int) (time % 1000 * 60 / 1000);
                String ampm = h < 12 ? "AM" : "PM";
                p.sendStatusMessage(new TextComponentString(String.format("§e🕰 It's %d:%02d %s §7(day %d)", h % 12 == 0 ? 12 : h % 12, m, ampm, w.getWorldTime() / 24000 + 1)), true);
                return true;
            }
            case GLOBE: {
                String[] places = {"Paris 🇫🇷", "Tokyo 🇯🇵", "Las Vegas 🎰", "Rio 🇧🇷", "Cairo 🇪🇬", "Sydney 🇦🇺", "Reykjavik 🇮🇸", "Toronto 🇨🇦",
                        "the middle of the ocean 🌊", "Antarctica 🐧", "Seoul 🇰🇷", "Mexico City 🇲🇽", "Amsterdam 🇳🇱", "Cape Town 🇿🇦", "Honolulu 🌺"};
                w.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.4F, 1.8F);
                p.sendStatusMessage(new TextComponentString("§b🌍 You spin the globe… it lands on §f" + places[w.rand.nextInt(places.length)]), true);
                return true;
            }
            case TANK:
                if (held.getItem() == Items.FISH || held.getItem() == Items.WHEAT_SEEDS) {
                    if (!p.capabilities.isCreativeMode) held.shrink(1);
                    ((net.minecraft.world.WorldServer) w).spawnParticle(EnumParticleTypes.WATER_BUBBLE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 15, 0.25, 0.2, 0.25, 0.02);
                    p.sendStatusMessage(new TextComponentString("§bThe fish swarm to the food. 🐟"), true);
                } else p.sendStatusMessage(new TextComponentString("§7Blub blub. 🐠 §8(feed them fish or seeds)"), true);
                return true;
            case DECOR:
                return false;
            default:
                if (def.slots > 0) { open(w, pos, p); return true; }
                return false;
        }
    }

    private static void open(World w, BlockPos pos, EntityPlayer p) {
        p.openGui(PrideFurniture.INSTANCE, 0, w, pos.getX(), pos.getY(), pos.getZ());
    }

    /** a sink: infinite water for buckets/bottles, washes dye off leather */
    private boolean sink(World w, BlockPos pos, EntityPlayer p, ItemStack held) {
        if (held.getItem() == Items.BUCKET) {
            held.shrink(1);
            give(p, new ItemStack(Items.WATER_BUCKET));
        } else if (held.getItem() == Items.GLASS_BOTTLE) {
            held.shrink(1);
            give(p, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER));
        } else if (held.getItem() instanceof ItemArmor && ((ItemArmor) held.getItem()).hasColor(held)) {
            ((ItemArmor) held.getItem()).removeColor(held);
            p.sendStatusMessage(new TextComponentString("§bWashed the dye out."), true);
        } else return false;
        w.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1.1F);
        return true;
    }

    static void wash(World w, BlockPos pos, EntityPlayer p, float amount, String msg) {
        Needs n = Needs.of(p);
        if (n != null) { n.hygiene = Math.min(100, n.hygiene + amount); Needs.sync(p); }
        w.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.4F, 1.5F);
        if (msg != null) p.sendStatusMessage(new TextComponentString(msg), true);
    }

    private static void give(EntityPlayer p, ItemStack s) { if (!p.addItemStackToInventory(s)) p.dropItem(s, false); }

    // ------------------------------------------------------------------ looks alive
    @Override public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        if (def.kind == Kind.SHOWER && s.getValue(ON)) {
            EnumFacing f = s.getValue(FACING);
            for (int i = 0; i < 6; i++) {
                double x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4, z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4;
                w.spawnParticle(EnumParticleTypes.WATER_DROP, x + f.getFrontOffsetX() * 0.1, pos.getY() + 1.6, z + f.getFrontOffsetZ() * 0.1, 0, -0.5, 0);
                w.spawnParticle(EnumParticleTypes.WATER_SPLASH, x, pos.getY() + 0.1, z, 0, 0.1, 0);
            }
            if (r.nextInt(3) == 0) w.playSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.WEATHER_RAIN, SoundCategory.BLOCKS, 0.25F, 1.6F, false);
        }
        if (def.kind == Kind.TUB && s.getValue(ON) && r.nextInt(3) == 0)
            w.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + r.nextDouble(), pos.getY() + 0.8, pos.getZ() + r.nextDouble(), 0, 0.02, 0);
        if (def.kind == Kind.FIREPLACE && s.getValue(ON)) {
            EnumFacing f = s.getValue(FACING);
            double fx = pos.getX() + 0.5 - f.getFrontOffsetX() * 0.1, fz = pos.getZ() + 0.5 - f.getFrontOffsetZ() * 0.1;
            for (int i = 0; i < 3; i++) w.spawnParticle(EnumParticleTypes.FLAME, fx + (r.nextDouble() - 0.5) * 0.4, pos.getY() + 0.2 + r.nextDouble() * 0.2, fz + (r.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, fx, pos.getY() + 0.6, fz, 0, 0.03, 0);
            if (r.nextInt(6) == 0) w.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.6F, 1F, false);
        }
        if (def.kind == Kind.TANK && r.nextInt(2) == 0)
            w.spawnParticle(EnumParticleTypes.WATER_BUBBLE, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.2, pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.05, 0);
        if (s.getValue(ON) && (def.kind == Kind.STOVE || def.kind == Kind.KETTLE || def.kind == Kind.COFFEE) && r.nextInt(2) == 0)
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + (def.kind == Kind.STOVE ? 1.05 : 0.6), pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.03, 0);
    }

    @Override public void addInformation(ItemStack stack, World w, List<String> tip, net.minecraft.client.util.ITooltipFlag flag) {
        String k = "tip.pridefurniture." + def.kind.name().toLowerCase();
        if (net.minecraft.client.resources.I18n.hasKey(k)) tip.add("§7" + net.minecraft.client.resources.I18n.format(k));
        if (def.has('c')) tip.add("§8Right-click it with a countertop to put one on.");
    }

    @Override public void onEntityWalk(World w, BlockPos pos, Entity e) {}
}
