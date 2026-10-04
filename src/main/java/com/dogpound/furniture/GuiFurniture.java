package com.dogpound.furniture;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

/** Pride-style panel: dark card, rainbow bar on top, soft slot wells, progress arrows for machines. */
public class GuiFurniture extends GuiContainer {
    static final int[] RAINBOW = {0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF004DFF, 0xFF750787};
    private final ContainerFurniture c;
    private final String title;

    public GuiFurniture(InventoryPlayer pinv, TileFurniture t) {
        super(new ContainerFurniture(pinv, t));
        c = (ContainerFurniture) inventorySlots;
        title = new net.minecraft.item.ItemStack(t.getBlockType()).getDisplayName();
        xSize = 176;
        ySize = c.height + 14 + 76 + 8;
    }

    @Override public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        super.drawScreen(mx, my, pt);
        renderHoveredToolTip(mx, my);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        int x = guiLeft, y = guiTop;
        drawRect(x - 2, y - 2, x + xSize + 2, y + ySize + 2, 0xC0000000);
        drawGradientRect(x, y, x + xSize, y + ySize, 0xF02A1F3D, 0xF0181226);
        int seg = xSize / RAINBOW.length;
        for (int i = 0; i < RAINBOW.length; i++) drawRect(x + i * seg, y, i == RAINBOW.length - 1 ? x + xSize : x + (i + 1) * seg, y + 3, RAINBOW[i]);
        drawRect(x + 4, y + c.height + 9, x + xSize - 4, y + c.height + 10, 0x40FFFFFF);       // divider above the player's inventory
        for (Slot s : inventorySlots.inventorySlots) {
            drawRect(x + s.xPos - 1, y + s.yPos - 1, x + s.xPos + 17, y + s.yPos + 17, 0xFF120D1C);
            drawRect(x + s.xPos, y + s.yPos, x + s.xPos + 16, y + s.yPos + 16, 0xFF3A3150);
        }
        for (int l = 0; l < c.lanes.length; l++) {                                              // arrow: grey track, pink fill
            int ay = y + c.top + l * 22 + 5, ax = x + 66, w = 44;
            drawRect(ax, ay + 2, ax + w, ay + 6, 0xFF2A2338);
            int fill = c.progress[l] * w / Math.max(1, TileMachine.time(c.def.kind));
            drawRect(ax, ay + 2, ax + fill, ay + 6, 0xFFEF7FBF);
            drawRect(ax + w, ay, ax + w + 4, ay + 8, fill >= w ? 0xFFEF7FBF : 0xFF2A2338);
        }
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawStringWithShadow(title, 8, 7, 0xFFFFFF);
        fontRenderer.drawString("Inventory", 8, c.height + 3, 0xB0A8C8);
        if (c.lanes.length > 0) fontRenderer.drawString(hint(), 8, c.height - 4 > 12 ? c.height - 2 : c.height, 0x8F86A8);
    }

    private String hint() {
        switch (c.def.kind) {
            case STOVE: return "Cooks any raw food";
            case MICROWAVE: return "Zaps food fast";
            case TOASTER: return "Bread → Toast";
            case COFFEE: return "Cocoa + Water Bottle";
            case BLENDER: return "Fruit or veggies → Smoothie";
            case KETTLE: return "Water Bottle → Tea";
            case WASHER: return "Washes dye out";
            case DRYER: return "Wet Sponge → Sponge";
            default: return "";
        }
    }
}
