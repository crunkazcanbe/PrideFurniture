package com.dogpound.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/** Bladder and bowel fill up (0 = fine, 100 = can't wait), hygiene drains (100 = fresh, 0 = stinky). */
public class Needs {
    @CapabilityInject(Needs.class)
    public static Capability<Needs> CAP;

    public float bladder = 10, bowel = 5, hygiene = 100;

    public static Needs of(EntityPlayer p) { return CAP == null ? null : p.getCapability(CAP, null); }

    public static void sync(EntityPlayer p) {
        Needs n = of(p);
        if (n != null && p instanceof EntityPlayerMP) PrideFurniture.NET.sendTo(new NeedsMsg(n), (EntityPlayerMP) p);
    }

    /** sitting on the toilet and right-clicking it */
    static void business(EntityPlayer p, BlockPos pos) {
        Needs n = of(p);
        if (n == null) return;
        boolean pee = n.bladder >= 15, poop = n.bowel >= 25;
        if (!PrideFurniture.Settings.needs) pee = poop = true;
        if (!pee && !poop) { p.sendStatusMessage(new TextComponentString("§7You don't really need to go right now."), true); return; }
        WorldServer w = (WorldServer) p.world;
        String what;
        if (pee && poop) what = "§eYou did both. Big relief. 😌";
        else if (poop) what = "§6You pooped. 💩";
        else what = "§eYou peed. Ahh. 💛";
        if (pee) { n.bladder = 0; w.playSound(null, pos, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.PLAYERS, 0.6F, 1.8F); }
        if (poop) { n.bowel = 0; w.playSound(null, pos, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.5F, 0.6F); }
        w.spawnParticle(EnumParticleTypes.WATER_SPLASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.15, 0.05, 0.15, 0.1);
        n.hygiene = Math.max(0, n.hygiene - 5);
        p.sendStatusMessage(new TextComponentString(what + " §7Sneak to stand up and flush."), true);
        if (p.getRidingEntity() instanceof EntitySeat) ((EntitySeat) p.getRidingEntity()).markUsed();
        sync(p);
    }

    static void weighIn(EntityPlayer p) {
        int items = 0;
        for (int i = 0; i < p.inventory.getSizeInventory(); i++) items += p.inventory.getStackInSlot(i).getCount();
        Needs n = of(p);
        p.sendMessage(new TextComponentString("§d⚖ §fHealth §c" + (int) p.getHealth() + "/" + (int) p.getMaxHealth()
                + " §7· §fFood §6" + p.getFoodStats().getFoodLevel() + "/20 §7· §fArmor §b" + p.getTotalArmorValue()
                + " §7· §fLevel §a" + p.experienceLevel + " §7· §fCarrying §e" + items + " items"));
        if (n != null) p.sendMessage(new TextComponentString("§d⚖ §fBladder §e" + (int) n.bladder + "% §7· §fBowel §6" + (int) n.bowel
                + "% §7· §fHygiene §b" + (int) n.hygiene + "%"));
    }

    public NBTTagCompound write() {
        NBTTagCompound t = new NBTTagCompound();
        t.setFloat("bladder", bladder); t.setFloat("bowel", bowel); t.setFloat("hygiene", hygiene);
        return t;
    }

    public void read(NBTTagCompound t) {
        if (t.hasKey("hygiene")) { bladder = t.getFloat("bladder"); bowel = t.getFloat("bowel"); hygiene = t.getFloat("hygiene"); }
    }

    public static class Storage implements Capability.IStorage<Needs> {
        @Override public NBTBase writeNBT(Capability<Needs> c, Needs n, EnumFacing f) { return n.write(); }
        @Override public void readNBT(Capability<Needs> c, Needs n, EnumFacing f, NBTBase t) { if (t instanceof NBTTagCompound) n.read((NBTTagCompound) t); }
    }
}
