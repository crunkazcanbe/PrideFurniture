package com.dogpound.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** An invisible seat for chairs, stools, the toilet and the bathtub. Gone as soon as nobody sits on it. */
public class EntitySeat extends Entity {
    private boolean used;     // the toilet remembers you went, so standing up flushes

    public EntitySeat(World w) {
        super(w);
        setSize(0.01F, 0.01F);
        noClip = true;
    }

    public static void sit(World w, BlockPos pos, EntityPlayer p, double height) {
        if (p.isRiding()) return;
        if (!w.getEntitiesWithinAABB(EntitySeat.class, new AxisAlignedBB(pos)).isEmpty()) return;   // someone's already there
        EntitySeat s = new EntitySeat(w);
        s.setPosition(pos.getX() + 0.5, pos.getY() + height, pos.getZ() + 0.5);
        w.spawnEntity(s);
        p.startRiding(s);
    }

    public BlockPos seatPos() { return new BlockPos(MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ)); }

    void markUsed() { used = true; }

    @Override public double getMountedYOffset() { return 0; }

    @Override public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        if (getPassengers().isEmpty() || !(world.getBlockState(seatPos()).getBlock() instanceof BlockFurniture)) setDead();
    }

    @Override protected void removePassenger(Entity e) {
        super.removePassenger(e);
        if (!world.isRemote && used) {
            world.playSound(null, seatPos(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.BLOCKS, 0.7F, 0.7F);
            world.playSound(null, seatPos(), SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 0.8F, 1.4F);
            if (e instanceof EntityPlayer) ((EntityPlayer) e).sendStatusMessage(new net.minecraft.util.text.TextComponentString("§b*flush* 🚽"), true);
            used = false;
        }
        e.setPosition(posX, posY + 0.6, posZ);
    }

    @Override protected void entityInit() {}
    @Override protected void readEntityFromNBT(NBTTagCompound t) {}
    @Override protected void writeEntityToNBT(NBTTagCompound t) {}
    @Override public boolean shouldRiderSit() { return true; }
}
