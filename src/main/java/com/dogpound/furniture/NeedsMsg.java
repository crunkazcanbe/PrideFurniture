package com.dogpound.furniture;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** server → client: the three needs, for the little HUD bars */
public class NeedsMsg implements IMessage {
    public static float bladder, bowel, hygiene = 100;     // client copy
    float a, b, c;

    public NeedsMsg() {}
    NeedsMsg(Needs n) { a = n.bladder; b = n.bowel; c = n.hygiene; }

    @Override public void fromBytes(ByteBuf buf) { a = buf.readFloat(); b = buf.readFloat(); c = buf.readFloat(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeFloat(a); buf.writeFloat(b); buf.writeFloat(c); }

    public static class Handler implements IMessageHandler<NeedsMsg, IMessage> {
        @Override public IMessage onMessage(NeedsMsg m, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> { bladder = m.a; bowel = m.b; hygiene = m.c; });
            return null;
        }
    }
}
