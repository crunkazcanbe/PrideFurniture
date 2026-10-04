package com.dogpound.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    @Override public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        TileEntity t = w.getTileEntity(new BlockPos(x, y, z));
        return t instanceof TileFurniture && ((TileFurniture) t).def() != null ? new ContainerFurniture(p.inventory, (TileFurniture) t) : null;
    }

    @Override public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        TileEntity t = w.getTileEntity(new BlockPos(x, y, z));
        return t instanceof TileFurniture && ((TileFurniture) t).def() != null ? new GuiFurniture(p.inventory, (TileFurniture) t) : null;
    }
}
