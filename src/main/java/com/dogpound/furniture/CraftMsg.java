package com.dogpound.furniture;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** client → server: "make me this piece of furniture, n times" (server checks the workbench is in reach) */
public class CraftMsg implements IMessage {
    String id; int times;

    public CraftMsg() {}
    public CraftMsg(String id, int times) { this.id = id; this.times = times; }

    @Override public void fromBytes(ByteBuf b) { id = ByteBufUtils.readUTF8String(b); times = b.readInt(); }
    @Override public void toBytes(ByteBuf b) { ByteBufUtils.writeUTF8String(b, id); b.writeInt(times); }

    public static class Handler implements IMessageHandler<CraftMsg, IMessage> {
        @Override public IMessage onMessage(CraftMsg m, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                if (!nearWorkbench(p)) return;
                WorkbenchRecipes.craft(p, m.id, Math.max(1, Math.min(64, m.times)));
            });
            return null;
        }
    }

    static boolean nearWorkbench(EntityPlayerMP p) {
        net.minecraft.block.Block wb = PrideFurniture.BLOCKS.get("furniture_workbench");
        for (net.minecraft.util.math.BlockPos pos : net.minecraft.util.math.BlockPos.getAllInBox(p.getPosition().add(-6, -3, -6), p.getPosition().add(6, 3, 6)))
            if (p.world.getBlockState(pos).getBlock() == wb) return true;
        return false;
    }
}
