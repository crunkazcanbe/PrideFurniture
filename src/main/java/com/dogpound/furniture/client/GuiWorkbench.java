package com.dogpound.furniture.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.dogpound.furniture.CraftMsg;
import com.dogpound.furniture.PrideFurniture;
import com.dogpound.furniture.WorkbenchRecipes;
import com.dogpound.furniture.WorkbenchRecipes.Ing;
import com.dogpound.furniture.WorkbenchRecipes.Recipe;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.client.audio.PositionedSoundRecord;

/**
 * The Furniture Workbench catalog: categories on the left, every piece as a tile in the middle (search on top),
 * the chosen piece big on the right with what it needs (green = you have enough, red = missing) and Craft buttons.
 */
public class GuiWorkbench extends GuiScreen {
    static final int[] RAINBOW = {0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF004DFF, 0xFF750787};
    static final int PANEL_TOP = 0xF02A1F3D, PANEL_BOT = 0xF0181226, TILE = 0xFF3A3150, TILE_HI = 0xFF54476F, PINK = 0xFFEF7FBF;
    static final int CELL = 22;

    private final List<String> cats = new ArrayList<>();
    private String cat = "All";
    private final List<Recipe> shown = new ArrayList<>();
    private Recipe sel;
    private GuiTextField search;
    private int scroll;
    private int px, py, pw, ph, gridX, gridY, gridW, gridH, cols, detailX;
    private long ticks;

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        cats.clear();
        cats.add("All");
        cats.addAll(WorkbenchRecipes.categories());
        pw = Math.min(width - 16, 460); ph = Math.min(height - 16, 260);
        px = (width - pw) / 2; py = (height - ph) / 2;
        int catW = 86, detailW = 140;
        gridX = px + catW + 10; gridY = py + 40;
        gridW = pw - catW - detailW - 24; gridH = ph - 50;
        cols = Math.max(1, gridW / CELL);
        detailX = px + pw - detailW - 6;
        Keyboard.enableRepeatEvents(true);
        search = new GuiTextField(0, fontRenderer, gridX + 2, py + 22, gridW - 4, 12);
        search.setMaxStringLength(40);
        search.setFocused(true);
        refresh();
    }

    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    private void refresh() {
        shown.clear();
        String q = search == null ? "" : search.getText().toLowerCase().trim();
        for (Recipe r : WorkbenchRecipes.all()) {
            if (!cat.equals("All") && !r.category.equals(cat)) continue;
            if (!q.isEmpty() && !r.output().getDisplayName().toLowerCase().contains(q)) continue;
            shown.add(r);
        }
        if (sel == null || !shown.contains(sel)) sel = shown.isEmpty() ? null : shown.get(0);
        scroll = 0;
    }

    @Override public void updateScreen() { ticks++; search.updateCursorCounter(); }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        drawRect(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xC0000000);
        drawGradientRect(px, py, px + pw, py + ph, PANEL_TOP, PANEL_BOT);
        int seg = pw / RAINBOW.length;
        for (int i = 0; i < RAINBOW.length; i++) drawRect(px + i * seg, py, i == RAINBOW.length - 1 ? px + pw : px + (i + 1) * seg, py + 3, RAINBOW[i]);
        fontRenderer.drawStringWithShadow("🪚 Furniture Workbench", px + 8, py + 8, 0xFFFFFF);
        String count = shown.size() + " pieces";
        fontRenderer.drawString(count, gridX + gridW - fontRenderer.getStringWidth(count), py + 8, 0x9F96B8);

        // categories
        for (int i = 0; i < cats.size(); i++) {
            int y = py + 22 + i * 18;
            boolean on = cats.get(i).equals(cat), hover = mx >= px + 6 && mx < px + 86 && my >= y && my < y + 16;
            drawRect(px + 6, y, px + 86, y + 16, on ? TILE_HI : hover ? 0xFF453A5C : TILE);
            if (on) drawRect(px + 6, y, px + 8, y + 16, RAINBOW[i % RAINBOW.length]);
            fontRenderer.drawString(cats.get(i), px + 12, y + 4, on ? 0xFFFFFF : 0xC8C0DC);
        }

        // search box
        drawRect(gridX, py + 20, gridX + gridW, py + 35, 0xFF120D1C);
        search.drawTextBox();
        if (search.getText().isEmpty() && !search.isFocused()) fontRenderer.drawString("Search…", gridX + 4, py + 24, 0x6F668A);

        // grid
        int rowsVisible = gridH / CELL, total = (shown.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - rowsVisible)));
        Recipe hover = null;
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = scroll * cols; i < shown.size() && i < (scroll + rowsVisible) * cols; i++) {
            int c = i % cols, r = i / cols - scroll;
            int x = gridX + c * CELL, y = gridY + r * CELL;
            Recipe rec = shown.get(i);
            boolean h = mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2;
            if (h) hover = rec;
            boolean can = WorkbenchRecipes.canMake(mc.player, rec) > 0;
            drawRect(x, y, x + CELL - 2, y + CELL - 2, rec == sel ? PINK : h ? TILE_HI : TILE);
            if (!can) drawRect(x, y + CELL - 4, x + CELL - 2, y + CELL - 2, 0xFF8A2A3A);
            mc.getRenderItem().renderItemAndEffectIntoGUI(rec.output(), x + 2, y + 2);
        }
        RenderHelper.disableStandardItemLighting();
        if (total > rowsVisible) {                                   // scrollbar
            int bh = Math.max(12, gridH * rowsVisible / total), by = gridY + (gridH - bh) * scroll / Math.max(1, total - rowsVisible);
            drawRect(gridX + gridW + 1, gridY, gridX + gridW + 3, gridY + gridH, 0xFF120D1C);
            drawRect(gridX + gridW + 1, by, gridX + gridW + 3, by + bh, PINK);
        }

        drawDetail(mx, my);
        if (hover != null) drawHoveringText(hover.output().getDisplayName(), mx, my);
        super.drawScreen(mx, my, pt);
    }

    private int craftY1() { return py + ph - 26; }

    private void drawDetail(int mx, int my) {
        int x = detailX, w = px + pw - 6 - x;
        drawRect(x, py + 20, x + w, py + ph - 6, 0x60000000);
        if (sel == null) { fontRenderer.drawString("Nothing here", x + 8, py + 30, 0x9F96B8); return; }
        ItemStack out = sel.output();
        // big 3D preview, slowly turning
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + w / 2F - 24, py + 26, 100);
        GlStateManager.scale(3, 3, 3);
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(out, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        String name = out.getDisplayName() + (sel.count > 1 ? " ×" + sel.count : "");
        List<String> lines = fontRenderer.listFormattedStringToWidth(name, w - 8);
        int y = py + 78;
        for (String l : lines) { fontRenderer.drawStringWithShadow(l, x + (w - fontRenderer.getStringWidth(l)) / 2F, y, 0xFFFFFF); y += 10; }
        String tipKey = "tip.pridefurniture." + tipFor(out);
        if (I18n.hasKey(tipKey)) for (String l : fontRenderer.listFormattedStringToWidth(I18n.format(tipKey), w - 8)) { fontRenderer.drawString(l, x + 4, y, 0x9F96B8); y += 9; }
        y += 4;
        fontRenderer.drawString("Needs:", x + 4, y, 0xC8C0DC);
        y += 11;
        RenderHelper.enableGUIStandardItemLighting();
        for (Ing i : sel.ings) {
            if (y > craftY1() - 18) break;
            ItemStack show = i.show(ticks);
            mc.getRenderItem().renderItemAndEffectIntoGUI(show, x + 4, y);
            int have = WorkbenchRecipes.have(mc.player, i);
            boolean ok = have >= i.count || mc.player.capabilities.isCreativeMode;
            String label = show.isEmpty() ? i.ore : show.getDisplayName();
            label = fontRenderer.trimStringToWidth(label, w - 52);
            fontRenderer.drawString(label, x + 23, y + 1, 0xDDDDEE);
            fontRenderer.drawString((ok ? "§a" : "§c") + Math.min(have, 999) + "/" + i.count, x + 23, y + 9, 0xFFFFFF);
            y += 19;
        }
        RenderHelper.disableStandardItemLighting();
        int can = WorkbenchRecipes.canMake(mc.player, sel);
        button(x + 4, craftY1(), (w - 12) / 2, 20, "Craft", can > 0, mx, my);
        button(x + 8 + (w - 12) / 2, craftY1(), (w - 12) / 2, 20, "Craft ×5", can >= 5, mx, my);
    }

    private String tipFor(ItemStack out) {
        net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromItem(out.getItem());
        return b instanceof com.dogpound.furniture.BlockFurniture ? ((com.dogpound.furniture.BlockFurniture) b).def.kind.name().toLowerCase() : "none";
    }

    private void button(int x, int y, int w, int h, String label, boolean enabled, int mx, int my) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        drawRect(x, y, x + w, y + h, !enabled ? 0xFF2A2338 : hov ? 0xFFF59BCD : PINK);
        fontRenderer.drawStringWithShadow(label, x + (w - fontRenderer.getStringWidth(label)) / 2F, y + 6, enabled ? 0xFFFFFF : 0x6F668A);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        search.mouseClicked(mx, my, btn);
        for (int i = 0; i < cats.size(); i++) {
            int y = py + 22 + i * 18;
            if (mx >= px + 6 && mx < px + 86 && my >= y && my < y + 16) { cat = cats.get(i); refresh(); click(); return; }
        }
        if (mx >= gridX && mx < gridX + cols * CELL && my >= gridY && my < gridY + gridH) {
            int i = (scroll + (my - gridY) / CELL) * cols + (mx - gridX) / CELL;
            if (i >= 0 && i < shown.size()) { sel = shown.get(i); click(); }
            return;
        }
        if (sel != null && my >= craftY1() && my < craftY1() + 20) {
            int x = detailX, w = px + pw - 6 - x, half = (w - 12) / 2;
            int can = WorkbenchRecipes.canMake(mc.player, sel);
            if (mx >= x + 4 && mx < x + 4 + half && can > 0) PrideFurniture.NET.sendToServer(new CraftMsg(sel.id, 1));
            else if (mx >= x + 8 + half && mx < x + 8 + 2 * half && can >= 5) PrideFurniture.NET.sendToServer(new CraftMsg(sel.id, 5));
            else return;
            click();
        }
    }

    private void click() { mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (search.textboxKeyTyped(c, key)) { refresh(); return; }
        super.keyTyped(c, key);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll -= Integer.signum(d);
    }
}
