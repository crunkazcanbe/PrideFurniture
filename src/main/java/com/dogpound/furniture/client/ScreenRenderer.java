package com.dogpound.furniture.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.lwjgl.opengl.GL11;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.dogpound.furniture.BlockFurniture;
import com.dogpound.furniture.PrideFurniture;
import com.dogpound.furniture.TileMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/**
 * The smart fridge's screen plays GIFs. Built-in ones ship in the jar; drop your own .gif files into
 * config/pridefurniture/screens/ and sneak + right-click the fridge to flip through them.
 */
public class ScreenRenderer extends TileEntitySpecialRenderer<TileMachine> {
    private static final double[] FRIDGE = {1.3, 5.8, 18.8, 29.2, 0.43}, TV = {-3.5, 19.5, 2.5, 15, 8.83}, PC = {1.5, 14.5, 4.5, 12.5, 9.83};
    private static final String[] BUILT_IN = {"pride", "weather", "aquarium"};

    static final class Gif {
        final List<Integer> textures = new ArrayList<>();
        final List<Integer> delays = new ArrayList<>();
        int total;
        int frameAt(long ms) {
            long t = ms % Math.max(1, total);
            for (int i = 0; i < delays.size(); i++) { t -= delays.get(i); if (t < 0) return textures.get(i); }
            return textures.get(textures.size() - 1);
        }
    }

    private static List<Gif> gifs;

    private static List<Gif> gifs() {
        if (gifs != null) return gifs;
        gifs = new ArrayList<>();
        for (String n : BUILT_IN) {
            try (InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(PrideFurniture.ID, "screens/" + n + ".gif")).getInputStream()) {
                Gif g = load(ImageIO.createImageInputStream(in));
                if (g != null) gifs.add(g);
            } catch (Exception e) { System.out.println("[PrideFurniture] screen gif " + n + ": " + e); }
        }
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config/pridefurniture/screens");
        dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".gif"));
        if (files != null) for (File f : files) {
            try { Gif g = load(ImageIO.createImageInputStream(f)); if (g != null) gifs.add(g); }
            catch (Exception e) { System.out.println("[PrideFurniture] screen gif " + f.getName() + ": " + e); }
        }
        return gifs;
    }

    /** decodes every frame onto one canvas (so partial frames layer correctly), max 128 px tall */
    private static Gif load(ImageInputStream in) throws Exception {
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
        if (!it.hasNext() || in == null) return null;
        ImageReader r = it.next();
        r.setInput(in);
        int n = r.getNumImages(true);
        BufferedImage canvas = null;
        Gif g = new Gif();
        for (int i = 0; i < Math.min(n, 120); i++) {
            BufferedImage f = r.read(i);
            int x = 0, y = 0, delay = 100;
            IIOMetadata md = r.getImageMetadata(i);
            Node root = md.getAsTree("javax_imageio_gif_image_1.0");
            for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
                NamedNodeMap a = c.getAttributes();
                if (c.getNodeName().equals("ImageDescriptor")) {
                    x = Integer.parseInt(a.getNamedItem("imageLeftPosition").getNodeValue());
                    y = Integer.parseInt(a.getNamedItem("imageTopPosition").getNodeValue());
                } else if (c.getNodeName().equals("GraphicControlExtension")) {
                    delay = Math.max(20, Integer.parseInt(a.getNamedItem("delayTime").getNodeValue()) * 10);
                }
            }
            if (canvas == null) canvas = new BufferedImage(Math.max(f.getWidth() + x, 1), Math.max(f.getHeight() + y, 1), BufferedImage.TYPE_INT_ARGB);
            Graphics2D gr = canvas.createGraphics();
            gr.drawImage(f, x, y, null);
            gr.dispose();
            BufferedImage shown = canvas;
            if (canvas.getHeight() > 128) {
                int h = 128, w = Math.max(1, canvas.getWidth() * 128 / canvas.getHeight());
                shown = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D s = shown.createGraphics();
                s.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                s.drawImage(canvas, 0, 0, w, h, null);
                s.dispose();
            }
            g.textures.add(new DynamicTexture(shown).getGlTextureId());
            g.delays.add(delay);
            g.total += delay;
        }
        r.dispose();
        return g.textures.isEmpty() ? null : g;
    }

    @Override
    public void render(TileMachine te, double x, double y, double z, float pt, int destroy, float alpha) {
        IBlockState s = te.getWorld().getBlockState(te.getPos());
        if (!(s.getBlock() instanceof BlockFurniture)) return;
        String id = ((BlockFurniture) s.getBlock()).def.id;
        double[] r = id.equals("fridge_smart") ? FRIDGE : id.equals("tv") ? TV : id.equals("computer") ? PC : null;   // x0 x1 y0 y1 z (pixels)
        if (r == null || !id.equals("fridge_smart") && !s.getValue(BlockFurniture.ON)) return;
        List<Gif> all = gifs();
        if (all.isEmpty()) return;
        Gif g = all.get(Math.floorMod(te.screen, all.size()));
        EnumFacing f = s.getValue(BlockFurniture.FACING);
        float yRot = f == EnumFacing.EAST ? 90 : f == EnumFacing.SOUTH ? 180 : f == EnumFacing.WEST ? 270 : 0;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(-yRot, 0, 1, 0);
        GlStateManager.translate(-0.5, 0, -0.5);
        GlStateManager.bindTexture(g.frameAt(System.currentTimeMillis()));
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);   // screens glow
        GlStateManager.color(1, 1, 1, 1);
        double x0 = r[0] / 16, x1 = r[1] / 16, y0 = r[2] / 16, y1 = r[3] / 16, zz = r[4] / 16;
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        b.pos(x1, y1, zz).tex(0, 0).endVertex();
        b.pos(x1, y0, zz).tex(0, 1).endVertex();
        b.pos(x0, y0, zz).tex(1, 1).endVertex();
        b.pos(x0, y1, zz).tex(1, 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override public boolean isGlobalRenderer(TileMachine te) { return false; }
}
