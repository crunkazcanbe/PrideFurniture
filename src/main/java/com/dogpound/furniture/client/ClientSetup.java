package com.dogpound.furniture.client;

import com.dogpound.furniture.BlockFurniture;
import com.dogpound.furniture.EntitySeat;
import com.dogpound.furniture.NeedsMsg;
import com.dogpound.furniture.PrideFurniture;
import com.dogpound.furniture.TileMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = PrideFurniture.ID)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (BlockFurniture b : PrideFurniture.BLOCKS.values())
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0, new ModelResourceLocation(b.getRegistryName(), "inventory"));
        for (Item i : PrideFurniture.ITEMS.values())
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        ModelLoader.setCustomStateMapper(PrideFurniture.FILLER, new StateMapperBase() {
            @Override protected ModelResourceLocation getModelResourceLocation(IBlockState s) {
                return new ModelResourceLocation(new ResourceLocation(PrideFurniture.ID, "filler"), "normal");
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntitySeat.class, m -> new Render<EntitySeat>(m) {
            @Override protected ResourceLocation getEntityTexture(EntitySeat s) { return null; }
            @Override public boolean shouldRender(EntitySeat s, net.minecraft.client.renderer.culling.ICamera c, double x, double y, double z) { return false; }
        });
        ClientRegistry.bindTileEntitySpecialRenderer(TileMachine.class, new ScreenRenderer());
    }

    public static void openWorkbench() { Minecraft.getMinecraft().displayGuiScreen(new GuiWorkbench()); }

    /** three tiny bars: bladder (yellow), bowel (brown), hygiene (blue). PrideHUD picks them up into its strip. */
    @SubscribeEvent
    public static void hud(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL || !PrideFurniture.Settings.needs || !PrideFurniture.Settings.needsHud) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator() || mc.gameSettings.hideGUI) return;
        ScaledResolution sr = e.getResolution();
        int x = sr.getScaledWidth() / 2 + 96, y = sr.getScaledHeight() - 20;
        bar(mc, x, y, NeedsMsg.bladder, 0xFFF2D24B, "💧", true);
        bar(mc, x, y + 6, NeedsMsg.bowel, 0xFF9A6234, "🍑", true);
        bar(mc, x, y + 12, NeedsMsg.hygiene, 0xFF6FC3F0, "🫧", false);
    }

    private static void bar(Minecraft mc, int x, int y, float v, int col, String icon, boolean fillingIsBad) {
        int w = 40, f = (int) (w * Math.max(0, Math.min(100, v)) / 100);
        boolean warn = fillingIsBad ? v >= 80 : v <= 20;
        Gui.drawRect(x, y, x + w + 2, y + 5, 0xA0000000);
        Gui.drawRect(x + 1, y + 1, x + 1 + f, y + 4, warn && (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFF4040 : col);
    }

}
