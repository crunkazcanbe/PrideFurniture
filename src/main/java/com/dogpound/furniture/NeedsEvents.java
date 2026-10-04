package com.dogpound.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Needs slowly change; eating fills the bowel, drinking fills the bladder; going too long has small effects. */
public class NeedsEvents {
    private static final ResourceLocation KEY = new ResourceLocation(PrideFurniture.ID, "needs");

    @SubscribeEvent
    public void attach(AttachCapabilitiesEvent<Entity> e) {
        if (!(e.getObject() instanceof EntityPlayer)) return;
        Needs n = new Needs();
        e.addCapability(KEY, new ICapabilitySerializable<NBTTagCompound>() {
            @Override public boolean hasCapability(Capability<?> c, EnumFacing f) { return c == Needs.CAP; }
            @Override public <T> T getCapability(Capability<T> c, EnumFacing f) { return c == Needs.CAP ? Needs.CAP.cast(n) : null; }
            @Override public NBTTagCompound serializeNBT() { return n.write(); }
            @Override public void deserializeNBT(NBTTagCompound t) { n.read(t); }
        });
    }

    @SubscribeEvent
    public void clone(PlayerEvent.Clone e) {
        Needs a = Needs.of(e.getOriginal()), b = Needs.of(e.getEntityPlayer());
        if (a != null && b != null) b.read(a.write());
    }

    @SubscribeEvent
    public void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) { Needs.sync(e.player); }

    @SubscribeEvent
    public void used(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntityLiving() instanceof EntityPlayer) || e.getEntityLiving().world.isRemote) return;
        EntityPlayer p = (EntityPlayer) e.getEntityLiving();
        Needs n = Needs.of(p);
        ItemStack s = e.getItem();
        if (n == null) return;
        if (s.getItemUseAction() == EnumAction.DRINK || s.getItem() == Items.MILK_BUCKET) n.bladder = Math.min(100, n.bladder + 18);
        if (s.getItem() instanceof ItemFood) n.bowel = Math.min(100, n.bowel + ((ItemFood) s.getItem()).getHealAmount(s) * 1.6F);
        Needs.sync(p);
    }

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent e) {
        EntityPlayer p = e.player;
        if (e.phase != TickEvent.Phase.END || p.world.isRemote || !PrideFurniture.Settings.needs || p.isCreative() || p.isSpectator()) return;
        if (p.ticksExisted % 100 != 0) return;                      // every 5 seconds
        Needs n = Needs.of(p);
        if (n == null) return;
        float sp = (float) PrideFurniture.Settings.needsSpeed;
        n.bladder = Math.min(100, n.bladder + 0.17F * sp);           // ~50 minutes from empty to bursting
        n.bowel = Math.min(100, n.bowel + 0.07F * sp);
        n.hygiene = Math.max(0, n.hygiene - (p.isInWater() ? -3 : 0.08F * sp));
        if (p.isInWater() && n.hygiene > 100) n.hygiene = 100;
        if (n.bladder >= 90 || n.bowel >= 90) {
            p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 0, true, false));
            if (p.ticksExisted % 1200 == 0) p.sendStatusMessage(new TextComponentString("§eYou really need to find a toilet! 🚽"), true);
        }
        if (n.hygiene <= 15 && p.world instanceof WorldServer)
            ((WorldServer) p.world).spawnParticle(EnumParticleTypes.TOWN_AURA, p.posX, p.posY + 1.5, p.posZ, 6, 0.3, 0.3, 0.3, 0);
        if (n.hygiene <= 15 && p.ticksExisted % 1200 == 0) p.sendStatusMessage(new TextComponentString("§2You're getting stinky. Time for a shower! 🚿"), true);
        if (p.ticksExisted % 200 == 0) Needs.sync(p);
    }
}
