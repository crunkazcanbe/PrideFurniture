package com.dogpound.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** The invisible second half of a 2-tall (fridge, shower) or 2-long (bathtub) piece. Everything goes to the main block. */
public class BlockFiller extends Block {
    public static final PropertyDirection TO_MAIN = PropertyDirection.create("to_main");

    public BlockFiller() {
        super(Material.IRON);
        setRegistryName(PrideFurniture.ID, "filler");
        setUnlocalizedName(PrideFurniture.ID + ".filler");
        setHardness(1.5F);
    }

    BlockPos main(IBlockAccess w, BlockPos pos) { return pos.offset(w.getBlockState(pos).getValue(TO_MAIN)); }

    private BlockFurniture mainBlock(IBlockAccess w, BlockPos pos) {
        Block b = w.getBlockState(main(w, pos)).getBlock();
        return b instanceof BlockFurniture ? (BlockFurniture) b : null;
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, TO_MAIN); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(TO_MAIN, EnumFacing.getFront(m & 7)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(TO_MAIN).getIndex(); }

    @Override public EnumBlockRenderType getRenderType(IBlockState s) { return EnumBlockRenderType.INVISIBLE; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        BlockFurniture m = mainBlock(w, pos);
        if (m == null) return FULL_BLOCK_AABB;
        AxisAlignedBB b = m.getBoundingBox(w.getBlockState(main(w, pos)), w, main(w, pos));
        if (s.getValue(TO_MAIN) == EnumFacing.DOWN) return new AxisAlignedBB(b.minX, 0, b.minZ, b.maxX, 1, b.maxZ);
        return new AxisAlignedBB(Math.min(b.minX, 0.0625), b.minY, Math.min(b.minZ, 0.0625), Math.max(b.maxX, 0.9375), b.maxY, Math.max(b.maxZ, 0.9375));
    }

    @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing side, float hx, float hy, float hz) {
        BlockFurniture m = mainBlock(w, pos);
        BlockPos mp = main(w, pos);
        return m != null && m.onBlockActivated(w, mp, w.getBlockState(mp), p, hand, side, hx, hy, hz);
    }

    @Override public void breakBlock(World w, BlockPos pos, IBlockState s) {
        BlockPos mp = pos.offset(s.getValue(TO_MAIN));
        if (w.getBlockState(mp).getBlock() instanceof BlockFurniture) w.destroyBlock(mp, true);
        super.breakBlock(w, pos, s);
    }

    @Override public int quantityDropped(java.util.Random r) { return 0; }

    @Override public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        BlockFurniture m = mainBlock(w, pos);
        return m == null ? ItemStack.EMPTY : new ItemStack(m);
    }
}
