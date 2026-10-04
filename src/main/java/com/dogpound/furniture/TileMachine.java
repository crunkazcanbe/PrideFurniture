package com.dogpound.furniture;

import java.util.List;

import com.dogpound.furniture.Defs.Kind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.items.ItemStackHandler;

/** Furniture that does something over time: cooking, washing, compacting, freezing, showers and baths. */
public class TileMachine extends TileFurniture implements ITickable {
    public final int[] progress = new int[4];
    private int compacted, tick;

    public static boolean ticks(Kind k) {
        switch (k) {
            case FRIDGE: case DISHWASHER: case COMPACTOR: case STOVE: case MICROWAVE: case TOASTER: case COFFEE:
            case BLENDER: case KETTLE: case WASHER: case DRYER: case SHOWER: case TUB: case SCREEN: return true;
            default: return false;
        }
    }

    /** cooking-style lanes: {input, extra input or -1, output}. Same table drives the GUI. */
    public static int[][] lanes(Kind k) {
        switch (k) {
            case STOVE: return new int[][] {{0, -1, 4}, {1, -1, 5}, {2, -1, 6}, {3, -1, 7}};
            case COFFEE: return new int[][] {{0, 1, 2}};
            case MICROWAVE: case TOASTER: case BLENDER: case KETTLE: case WASHER: case DRYER: return new int[][] {{0, -1, 1}};
            default: return new int[0][];
        }
    }

    static int time(Kind k) {
        switch (k) {
            case MICROWAVE: return 60;
            case TOASTER: return 80;
            case STOVE: return 160;
            case COFFEE: case KETTLE: return 120;
            case BLENDER: return 60;
            case WASHER: case DRYER: return 100;
            default: return 100;
        }
    }

    /** what a lane turns its input into (empty = can't) */
    static ItemStack result(Kind k, ItemStack in, ItemStack extra) {
        if (in.isEmpty()) return ItemStack.EMPTY;
        Item i = in.getItem();
        switch (k) {
            case STOVE: case MICROWAVE: {
                ItemStack r = FurnaceRecipes.instance().getSmeltingResult(in);
                return !r.isEmpty() && r.getItem() instanceof ItemFood ? r.copy() : ItemStack.EMPTY;
            }
            case TOASTER: return i == Items.BREAD ? item("toast") : ItemStack.EMPTY;
            case COFFEE:
                return i == Items.DYE && in.getMetadata() == EnumDyeColor.BROWN.getDyeDamage() && isWater(extra) ? item("coffee") : ItemStack.EMPTY;
            case BLENDER:
                return i == Items.APPLE || i == Items.MELON || i == Items.CARROT || i == Items.BEETROOT || i == Items.CHORUS_FRUIT
                        || i == Items.GOLDEN_APPLE || i.getRegistryName().getResourcePath().contains("berr") ? item("smoothie") : ItemStack.EMPTY;
            case KETTLE: return isWater(in) ? item("tea") : ItemStack.EMPTY;
            case WASHER:
                if (i instanceof ItemArmor && ((ItemArmor) i).hasColor(in)) { ItemStack c = in.copy(); c.setCount(1); ((ItemArmor) i).removeColor(c); return c; }
                if (i == Item.getItemFromBlock(Blocks.WOOL) && in.getMetadata() != 0) return new ItemStack(Blocks.WOOL);
                if (i == Item.getItemFromBlock(Blocks.CARPET) && in.getMetadata() != 0) return new ItemStack(Blocks.CARPET);
                return ItemStack.EMPTY;
            case DRYER: return i == Item.getItemFromBlock(Blocks.SPONGE) && in.getMetadata() == 1 ? new ItemStack(Blocks.SPONGE) : ItemStack.EMPTY;
            default: return ItemStack.EMPTY;
        }
    }

    static boolean isWater(ItemStack s) { return s.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(s) == PotionTypes.WATER; }
    static ItemStack item(String id) { return new ItemStack(PrideFurniture.ITEMS.get(id)); }

    @Override
    public void update() {
        if (world.isRemote) return;
        Defs.Def d = def();
        if (d == null) return;
        tick++;
        ItemStackHandler inv = inv();
        boolean busy = false;
        switch (d.kind) {
            case SCREEN: return;
            case SHOWER: if (world.getBlockState(pos).getValue(BlockFurniture.ON) && tick % 20 == 0) showerTick(); return;
            case TUB: if (world.getBlockState(pos).getValue(BlockFurniture.ON) && tick % 20 == 0) tubTick(); return;
            case FRIDGE: if (tick % 600 == 0) freeze(inv); return;
            case DISHWASHER: if (tick % 40 == 0) busy = dishwash(inv); break;
            case COMPACTOR: if (tick % 10 == 0) busy = compact(inv); break;
            default:
                int[][] lanes = lanes(d.kind);
                for (int l = 0; l < lanes.length; l++) busy |= lane(d.kind, inv, l, lanes[l]);
        }
        IBlockState s = world.getBlockState(pos);
        if (s.getValue(BlockFurniture.ON) != busy && (busy || tick % 20 == 0)) world.setBlockState(pos, s.withProperty(BlockFurniture.ON, busy), 2);
    }

    private boolean lane(Kind k, ItemStackHandler inv, int l, int[] lane) {
        ItemStack in = inv.getStackInSlot(lane[0]);
        ItemStack extra = lane[1] < 0 ? ItemStack.EMPTY : inv.getStackInSlot(lane[1]);
        ItemStack out = result(k, in, extra);
        if (out.isEmpty() || !inv.insertItem(lane[2], out, true).isEmpty()) { progress[l] = 0; return false; }
        if (++progress[l] < time(k)) return true;
        progress[l] = 0;
        inv.extractItem(lane[0], 1, false);
        if (lane[1] >= 0) { inv.extractItem(lane[1], 1, false); give(inv, new ItemStack(Items.GLASS_BOTTLE)); }
        if (k == Kind.KETTLE) give(inv, new ItemStack(Items.GLASS_BOTTLE));
        inv.insertItem(lane[2], out, false);
        world.playSound(null, pos, k == Kind.MICROWAVE ? SoundEvents.BLOCK_NOTE_BELL : k == Kind.TOASTER ? SoundEvents.BLOCK_PISTON_EXTEND
                : SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 0.6F, k == Kind.MICROWAVE ? 1.8F : 1.4F);
        return true;
    }

    /** leftover bottles go wherever there is room; dropped if the machine is full */
    private void give(ItemStackHandler inv, ItemStack s) {
        for (int i = 0; i < inv.getSlots() && !s.isEmpty(); i++) s = inv.insertItem(i, s, false);
        if (!s.isEmpty()) net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, s);
    }

    private boolean dishwash(ItemStackHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.getItem() == Items.GLASS_BOTTLE) {
                inv.setStackInSlot(i, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM, 1), PotionTypes.WATER));
                if (s.getCount() > 1) { ItemStack rest = s.copy(); rest.shrink(1); give(inv, rest); }
            } else if (s.getItem() instanceof ItemArmor && ((ItemArmor) s.getItem()).hasColor(s)) {
                ((ItemArmor) s.getItem()).removeColor(s);
            } else continue;
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.3F, 1.6F);
            return true;
        }
        return false;
    }

    private boolean compact(ItemStackHandler inv) {
        for (int i = 0; i < inv.getSlots() - 1; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) continue;
            compacted += s.getCount();
            inv.setStackInSlot(i, ItemStack.EMPTY);
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.4F, 0.6F);
            while (compacted >= 64 && inv.insertItem(inv.getSlots() - 1, item("trash_cube"), true).isEmpty()) {
                compacted -= 64;
                inv.insertItem(inv.getSlots() - 1, item("trash_cube"), false);
            }
            markDirty();
            return true;
        }
        return false;
    }

    /** the freezer: a water bucket becomes ice (the bucket goes back in if there's room) */
    private void freeze(ItemStackHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            if (inv.getStackInSlot(i).getItem() != Items.WATER_BUCKET) continue;
            inv.setStackInSlot(i, new ItemStack(Blocks.ICE));
            give(inv, new ItemStack(Items.BUCKET));
            return;
        }
    }

    private AxisAlignedBB around() { return new AxisAlignedBB(pos).expand(0, 1, 0); }

    private void showerTick() {
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, around())) {
            p.extinguish();
            Needs n = Needs.of(p);
            if (n != null) { n.hygiene = Math.min(100, n.hygiene + 8); Needs.sync(p); }
            clearBad(p);
            if (n != null && n.hygiene >= 100) p.addPotionEffect(new PotionEffect(MobEffects.SPEED, 20 * 120, 0, true, true));
        }
    }

    private void tubTick() {
        List<EntityPlayer> in = world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(0.5, 0.5, 0.5));
        for (EntityPlayer p : in) {
            if (!(p.getRidingEntity() instanceof EntitySeat)) continue;
            p.extinguish();
            p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 60, 0, true, false));
            Needs n = Needs.of(p);
            if (n != null) { n.hygiene = Math.min(100, n.hygiene + 5); Needs.sync(p); }
        }
    }

    static void clearBad(EntityPlayer p) {
        for (PotionEffect e : p.getActivePotionEffects().toArray(new PotionEffect[0])) {
            Potion pot = e.getPotion();
            if (pot.isBadEffect()) p.removePotionEffect(pot);
        }
    }

    @Override public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        compacted = t.getInteger("compacted");
        int[] p = t.getIntArray("progress");
        System.arraycopy(p, 0, progress, 0, Math.min(p.length, progress.length));
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        t.setInteger("compacted", compacted);
        t.setIntArray("progress", progress);
        return t;
    }
}
