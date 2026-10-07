package com.dogpound.furniture.client;

import com.dogpound.furniture.BlockGlossTile;
import com.dogpound.furniture.PrideFurniture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/**
 * Gloss tile reflections without shaders (requested feature). Each frame, for every creature standing on gloss tiles:
 *  1. stencil: mark the tile tops that are actually visible (bit 0) — walls/furniture in front still hide them;
 *  2. draw the creature again, flipped under the floor plane, only inside the marked area (marks bit 1 where drawn);
 *  3. lay the tile colour over the reflected pixels as a glaze, so it reads as polished tile, not a hole.
 * Only creatures within RANGE that stand on a tile are redrawn — no second world pass, so it's cheap.
 * Runs last in RenderWorldLast because it clears the depth buffer (the hand pass clears it right after anyway).
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = PrideFurniture.ID)
public final class GlossReflection {
    private GlossReflection() {}

    private static final int AROUND = 4;      // tile tops marked around each creature (its reflection can't reach farther)
    private static boolean stencilTried, stencilOk;
    private static long lastReport;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderWorldLastEvent e) {
        if (!PrideFurniture.Settings.glossReflections) return;
        Minecraft mc = Minecraft.getMinecraft();
        World w = mc.world;
        Entity cam = mc.getRenderViewEntity();
        if (w == null || cam == null) return;
        if (!stencilTried) {
            stencilTried = true;
            Framebuffer fb = mc.getFramebuffer();
            stencilOk = fb.isStencilEnabled() || fb.enableStencil();
            if (!stencilOk) PrideFurniture.LOG.warn("[Pride Furniture] no stencil buffer here — gloss tiles won't reflect");
        }
        if (!stencilOk) return;

        float pt = e.getPartialTicks();
        RenderManager rm = mc.getRenderManager();
        // floor height -> creatures standing on gloss tiles at that height
        Map<Integer, List<Entity>> byFloor = new HashMap<>();
        double range = PrideFurniture.Settings.glossRange;
        boolean onlyMe = PrideFurniture.Settings.glossPlayerOnly;
        for (Entity en : w.getEntitiesWithinAABB(Entity.class, cam.getEntityBoundingBox().grow(range),
                x -> onlyMe ? x == mc.player : x instanceof EntityLivingBase || x instanceof EntityItem || x instanceof net.minecraft.entity.IProjectile)) {
            if (en.isInvisible() || en.getDistanceSq(cam) > range * range) continue;
            // (no frustum test here: Celeritas doesn't keep vanilla's ClippingHelper current, so it hid everyone)
            double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * pt;
            BlockPos p = new BlockPos(en.lastTickPosX + (en.posX - en.lastTickPosX) * pt, y - 0.05, en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * pt);
            for (int d = 0; d < 3; d++, p = p.down()) {            // standing on it, or hopping a little above it
                if (w.getBlockState(p).getBlock() instanceof BlockGlossTile) {
                    byFloor.computeIfAbsent(p.getY() + 1, k -> new ArrayList<>()).add(en);
                    break;
                }
                if (!w.isAirBlock(p)) break;
            }
        }
        if (System.currentTimeMillis() - lastReport > 10000) {             // short status line every 10 s while debugging
            lastReport = System.currentTimeMillis();
            int n = 0; for (List<Entity> l : byFloor.values()) n += l.size();
            PrideFurniture.LOG.info("[Pride Furniture] gloss: {} creature(s) on tiles at {} floor(s); stencil bits {}; fb stencil {}",
                    n, byFloor.size(), GL11.glGetInteger(GL11.GL_STENCIL_BITS), mc.getFramebuffer().isStencilEnabled());
        }
        if (byFloor.isEmpty()) return;

        double vx = rm.viewerPosX, vy = rm.viewerPosY, vz = rm.viewerPosZ;
        float glaze = 1F - (float) PrideFurniture.Settings.glossStrength;
        GlStateManager.pushMatrix();
        try {
            for (Map.Entry<Integer, List<Entity>> f : byFloor.entrySet()) {
                int floorY = f.getKey();
                if (vy < floorY) continue;                          // looking at the floor from underneath
                Set<BlockPos> tiles = new HashSet<>();
                for (Entity en : f.getValue()) {
                    BlockPos c = new BlockPos(en.posX, floorY - 1, en.posZ);
                    for (int dx = -AROUND; dx <= AROUND; dx++)
                        for (int dz = -AROUND; dz <= AROUND; dz++) {
                            BlockPos t = c.add(dx, 0, dz);
                            if (w.getBlockState(t).getBlock() instanceof BlockGlossTile && !w.getBlockState(t.up()).isOpaqueCube()) tiles.add(t);
                        }
                }
                if (tiles.isEmpty()) continue;

                // 1. stencil bit 0 = visible tile top
                GL11.glEnable(GL11.GL_STENCIL_TEST);
                GL11.glStencilMask(0xFF);
                GL11.glClearStencil(0);
                GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                GlStateManager.colorMask(false, false, false, false);
                GlStateManager.depthMask(false);
                GlStateManager.enableDepth();
                GlStateManager.depthFunc(PrideFurniture.glossDebug == 3 ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
                GlStateManager.disableTexture2D();
                GlStateManager.disableCull();
                tops(tiles, w, vx, vy, vz, false, 0, range);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                if (PrideFurniture.glossDebug >= 2) {                // show the mask: red wherever bit 0 got set
                    GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0x01);
                    GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                    GlStateManager.disableDepth();
                    redTops(tiles, vx, vy, vz);
                    GlStateManager.enableDepth();
                    GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                }
                if (PrideFurniture.glossDebug == 1) GL11.glDisable(GL11.GL_STENCIL_TEST);

                // 2. mirrored creatures, only on visible tile; marks bit 1
                GlStateManager.depthMask(true);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glStencilFunc(GL11.GL_EQUAL, 3, 0x01);
                GL11.glStencilMask(0x02);
                GlStateManager.enableTexture2D();
                GlStateManager.enableCull();
                GL11.glFrontFace(GL11.GL_CW);                       // the flip turns triangles inside out
                RenderHelper.enableStandardItemLighting();
                mc.entityRenderer.enableLightmap();
                for (Entity en : f.getValue()) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(0, 2 * (floorY - vy), 0);
                    GlStateManager.scale(1, -1, 1);
                    GlStateManager.color(1, 1, 1, 1);
                    try { rm.renderEntityStatic(en, pt, false); } catch (RuntimeException ignored) {}   // one odd mob never breaks the frame
                    GlStateManager.popMatrix();
                }
                mc.entityRenderer.disableLightmap();
                RenderHelper.disableStandardItemLighting();
                GL11.glFrontFace(GL11.GL_CCW);

                // 3. glaze the tile colour over the reflection (bits 0+1)
                GL11.glStencilMask(0x00);
                GL11.glStencilFunc(GL11.GL_EQUAL, 3, 0x03);
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableCull();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                tops(tiles, w, vx, vy, vz, true, glaze, range);
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
        } finally {                                                // put back exactly what vanilla expects after this event
            GlStateManager.popMatrix();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glStencilMask(0xFF);
            GL11.glFrontFace(GL11.GL_CCW);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
        }
    }

    private static void redTops(Set<BlockPos> tiles, double vx, double vy, double vz) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (BlockPos p : tiles) {
            double x0 = p.getX() - vx, x1 = x0 + 1, z0 = p.getZ() - vz, z1 = z0 + 1, y = p.getY() + 1.002 - vy;
            b.pos(x0, y, z0).color(1F, 0F, 0F, 1F).endVertex(); b.pos(x0, y, z1).color(1F, 0F, 0F, 1F).endVertex();
            b.pos(x1, y, z1).color(1F, 0F, 0F, 1F).endVertex(); b.pos(x1, y, z0).color(1F, 0F, 0F, 1F).endVertex();
        }
        t.draw();
    }

    /** the top faces of these tiles, camera-relative; coloured = tile colour shaded by the light on it */
    private static void tops(Set<BlockPos> tiles, World w, double vx, double vy, double vz, boolean coloured, float glaze, double range) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (BlockPos p : tiles) {
            float r = 1, g = 1, bl = 1;
            if (coloured) {
                IBlockState s = w.getBlockState(p);
                int rgb = BlockGlossTile.tint(s.getValue(BlockGlossTile.COLOR));
                float light = 0.35F + 0.65F * w.getLightBrightness(p.up());
                r = ((rgb >> 16) & 255) / 255F * light; g = ((rgb >> 8) & 255) / 255F * light; bl = (rgb & 255) / 255F * light;
            }
            double x0 = p.getX() - vx, x1 = x0 + 1, z0 = p.getZ() - vz, z1 = z0 + 1, y = p.getY() + 1.001 - vy;
            // fade the reflection out over the last third of the range instead of popping off at the edge
            double dist = Math.sqrt((x0 + 0.5) * (x0 + 0.5) + y * y + (z0 + 0.5) * (z0 + 0.5));
            float fade = (float) Math.max(0, Math.min(1, (dist - range * 0.66) / (range * 0.34)));
            float alpha = glaze + (1 - glaze) * fade;
            b.pos(x0, y, z0).color(r, g, bl, alpha).endVertex();
            b.pos(x0, y, z1).color(r, g, bl, alpha).endVertex();
            b.pos(x1, y, z1).color(r, g, bl, alpha).endVertex();
            b.pos(x1, y, z0).color(r, g, bl, alpha).endVertex();
        }
        t.draw();
    }
}
