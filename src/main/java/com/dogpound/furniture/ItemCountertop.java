package com.dogpound.furniture;

import com.dogpound.furniture.BlockFurniture.Counter;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/** A countertop slab: right-click a base cabinet (or dishwasher, sink cabinet…) to set it on top. Sneak + empty hand takes it back. */
public class ItemCountertop extends Item {
    final Counter counter;

    public ItemCountertop(String c) { counter = Counter.of(c); }

    @Override
    public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing side, float hx, float hy, float hz) {
        Block b = w.getBlockState(pos).getBlock();
        if (!(b instanceof BlockFurniture) || !((BlockFurniture) b).def.has('c')) {
            if (!w.isRemote) p.sendStatusMessage(new TextComponentString("§7Countertops go on base cabinets, sink cabinets, dishwashers and compactors."), true);
            return EnumActionResult.FAIL;
        }
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof TileFurniture)) return EnumActionResult.FAIL;
        TileFurniture t = (TileFurniture) te;
        if (t.counter == counter) return EnumActionResult.PASS;
        if (!w.isRemote) {
            if (t.counter != Counter.NONE) {              // swapping: give the old one back
                net.minecraft.item.ItemStack old = new net.minecraft.item.ItemStack(PrideFurniture.ITEMS.get("countertop_" + t.counter.getName()));
                if (!p.addItemStackToInventory(old)) p.dropItem(old, false);
            }
            t.setCounter(counter);
            if (!p.capabilities.isCreativeMode) p.getHeldItem(hand).shrink(1);
            w.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1, 0.9F);
        }
        return EnumActionResult.SUCCESS;
    }
}
