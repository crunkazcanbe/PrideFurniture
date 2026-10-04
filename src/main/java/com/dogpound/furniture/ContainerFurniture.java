package com.dogpound.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Storage grids (rows of 9) or machine lanes (input → output), plus the player's inventory below. */
public class ContainerFurniture extends Container {
    public final TileFurniture tile;
    public final Defs.Def def;
    public final int[][] lanes;
    public final int rows, top, height;
    public final int[] progress = new int[4];

    public ContainerFurniture(InventoryPlayer pinv, TileFurniture tile) {
        this.tile = tile;
        this.def = tile.def();
        ItemStackHandler inv = tile.inv();
        lanes = TileMachine.lanes(def.kind);
        top = 20;
        if (lanes.length > 0) {
            rows = 0;
            for (int l = 0; l < lanes.length; l++) {
                int y = top + l * 22;
                addSlotToContainer(new SlotItemHandler(inv, lanes[l][0], 44, y));
                if (lanes[l][1] >= 0) addSlotToContainer(new SlotItemHandler(inv, lanes[l][1], 22, y));
                addSlotToContainer(new SlotItemHandler(inv, lanes[l][2], 116, y));
            }
            height = top + lanes.length * 22 + 6;
        } else {
            rows = (inv.getSlots() + 8) / 9;
            for (int i = 0; i < inv.getSlots(); i++) addSlotToContainer(new SlotItemHandler(inv, i, 8 + (i % 9) * 18, top + (i / 9) * 18));
            height = top + rows * 18 + 6;
        }
        int py = height + 14;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pinv, c + r * 9 + 9, 8 + c * 18, py + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pinv, c, 8 + c * 18, py + 58));
    }

    public int furnitureSlots() { return inventorySlots.size() - 36; }

    @Override public boolean canInteractWith(EntityPlayer p) {
        return !tile.isInvalid() && p.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64;
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot s = inventorySlots.get(index);
        if (s == null || !s.getHasStack()) return ItemStack.EMPTY;
        ItemStack st = s.getStack(), copy = st.copy();
        int n = furnitureSlots();
        if (index < n) { if (!mergeItemStack(st, n, inventorySlots.size(), true)) return ItemStack.EMPTY; }
        else if (!mergeItemStack(st, 0, n, false)) return ItemStack.EMPTY;
        if (st.isEmpty()) s.putStack(ItemStack.EMPTY); else s.onSlotChanged();
        return copy;
    }

    // machine progress → client
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(tile instanceof TileMachine)) return;
        int[] p = ((TileMachine) tile).progress;
        for (int i = 0; i < 4; i++) if (progress[i] != p[i]) {
            progress[i] = p[i];
            for (IContainerListener l : listeners) l.sendWindowProperty(this, i, p[i]);
        }
    }

    @Override public void updateProgressBar(int id, int v) { if (id >= 0 && id < 4) progress[id] = v; }
}
