package com.dogpound.furniture;

import com.dogpound.furniture.BlockFurniture.Counter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/** Storage, the countertop on top, the smart fridge's screen choice, the fruit bowl's timer. */
public class TileFurniture extends TileEntity {
    public final ItemStackHandler inv = new ItemStackHandler(0) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    public Counter counter = Counter.NONE;
    public int screen;
    private long fruitAt;

    public Defs.Def def() {
        Block b = world == null ? null : world.getBlockState(pos).getBlock();
        return b instanceof BlockFurniture ? ((BlockFurniture) b).def : null;
    }

    /** the inventory, sized for its furniture the first time anyone looks */
    public ItemStackHandler inv() {
        Defs.Def d = def();
        if (d != null && inv.getSlots() != d.slots && isEmpty()) inv.setSize(d.slots);
        return inv;
    }

    private boolean isEmpty() {
        for (int i = 0; i < inv.getSlots(); i++) if (!inv.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    public void setCounter(Counter c) {
        counter = c;
        sync();
    }

    public void sync() {
        markDirty();
        if (world != null) { IBlockState s = world.getBlockState(pos); world.notifyBlockUpdate(pos, s, s, 3); }
    }

    void fruit(EntityPlayer p) {
        long now = world.getTotalWorldTime();
        if (now - fruitAt < 20 * 60 * 5) {
            p.sendStatusMessage(new TextComponentString("§7The bowl's empty for now. More fruit in " + (20 * 60 * 5 - (now - fruitAt)) / 20 + "s."), true);
            return;
        }
        fruitAt = now;
        ItemStack s = new ItemStack(world.rand.nextInt(3) == 0 ? Items.MELON : Items.APPLE);
        if (!p.addItemStackToInventory(s)) p.dropItem(s, false);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 1.2F);
        markDirty();
    }

    @Override public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        if (t.hasKey("inv")) inv.deserializeNBT(t.getCompoundTag("inv"));
        counter = t.hasKey("counter") ? Counter.of(t.getString("counter")) : Counter.NONE;
        screen = t.getInteger("screen");
        fruitAt = t.getLong("fruitAt");
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        if (inv.getSlots() > 0) t.setTag("inv", inv.serializeNBT());
        t.setString("counter", counter.getName());
        t.setInteger("screen", screen);
        t.setLong("fruitAt", fruitAt);
        return t;
    }

    // client sync: countertop + screen (the inventory goes through the container)
    @Override public NBTTagCompound getUpdateTag() {
        NBTTagCompound t = super.writeToNBT(new NBTTagCompound());
        t.setString("counter", counter.getName());
        t.setInteger("screen", screen);
        return t;
    }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound t = pkt.getNbtCompound();
        counter = Counter.of(t.getString("counter"));
        screen = t.getInteger("screen");
        world.markBlockRangeForRenderUpdate(pos, pos);
    }
    @Override public void handleUpdateTag(NBTTagCompound t) {
        super.readFromNBT(t);
        counter = t.hasKey("counter") ? Counter.of(t.getString("counter")) : Counter.NONE;
        screen = t.getInteger("screen");
    }

    @Override public boolean shouldRefresh(World w, BlockPos p, IBlockState o, IBlockState n) { return o.getBlock() != n.getBlock(); }

    @Override public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() { return new net.minecraft.util.math.AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 3, 2)); }

    @Override public boolean hasCapability(Capability<?> cap, EnumFacing f) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && inv().getSlots() > 0 || super.hasCapability(cap, f);
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> cap, EnumFacing f) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && inv().getSlots() > 0 ? (T) inv() : super.getCapability(cap, f);
    }
}
