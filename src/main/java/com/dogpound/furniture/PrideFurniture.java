package com.dogpound.furniture;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Pride Furniture: kitchens, bathrooms and more — every piece modelled in Blockbench, most of them actually work.
 * Free and open for everyone.
 */
@Mod(modid = PrideFurniture.ID, name = "Pride Furniture", version = "0.1.0", acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber
public class PrideFurniture {
    public static final String ID = "pridefurniture";

    @Mod.Instance(ID)
    public static PrideFurniture INSTANCE;

    public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(ID);
    public static final Map<String, BlockFurniture> BLOCKS = new HashMap<>();
    public static BlockFiller FILLER;
    public static final Map<String, Item> ITEMS = new HashMap<>();
    public static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("Pride Furniture");
    public static BlockGlossTile GLOSS_TILE;
    /** /pfgloss <0-3>: 0 normal, 1 mirror without clipping, 2 paint the visible-tile mask red, 3 mask without depth test */
    public static volatile int glossDebug;

    public static final CreativeTabs TAB = new CreativeTabs(ID) {
        @Override public ItemStack getTabIconItem() { return new ItemStack(BLOCKS.get("fridge_smart")); }
    };

    @Config(modid = ID, name = "pridefurniture")
    public static class Settings {
        @Config.Comment("Bladder, bowel and hygiene needs (toilet, shower, bathtub, sink). Off = the furniture still works, you just never need it.")
        public static boolean needs = true;
        @Config.Comment("How fast needs change. 1.0 = normal, 0.5 = half as fast.")
        @Config.RangeDouble(min = 0.1, max = 5)
        public static double needsSpeed = 1.0;
        @Config.Comment("Show the three little needs bars on screen.")
        public static boolean needsHud = true;
        @Config.Comment("Gloss tiles show a mirror image of you and of mobs standing on them (no shaders needed).")
        public static boolean glossReflections = true;
        @Config.Comment("How strongly the reflection shows through a gloss tile. 0 = plain tile, 1 = a perfect mirror.")
        @Config.RangeDouble(min = 0, max = 1)
        public static double glossStrength = 0.45;
        @Config.Comment("How far away (blocks) gloss tiles still show reflections. They fade out over the last third.")
        @Config.RangeInt(min = 4, max = 64)
        public static int glossRange = 24;
        @Config.Comment("Performance mode: only YOU are reflected (no mobs, items or arrows).")
        public static boolean glossPlayerOnly = false;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        GameRegistry.registerTileEntity(TileFurniture.class, new ResourceLocation(ID, "furniture"));
        GameRegistry.registerTileEntity(TileMachine.class, new ResourceLocation(ID, "machine"));
        EntityRegistry.registerModEntity(new ResourceLocation(ID, "seat"), EntitySeat.class, "pf_seat", 0, this, 32, 20, false);
        CapabilityManager.INSTANCE.register(Needs.class, new Needs.Storage(), Needs::new);
        MinecraftForge.EVENT_BUS.register(new NeedsEvents());
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        NET.registerMessage(NeedsMsg.Handler.class, NeedsMsg.class, 0, Side.CLIENT);
        NET.registerMessage(CraftMsg.Handler.class, CraftMsg.class, 1, Side.SERVER);
    }

    @Mod.EventHandler
    public void serverStart(net.minecraftforge.fml.common.event.FMLServerStartingEvent e) {
        e.registerServerCommand(new net.minecraft.command.CommandBase() {
            @Override public String getName() { return "pfgloss"; }
            @Override public String getUsage(net.minecraft.command.ICommandSender s) { return "/pfgloss <0-3>"; }
            @Override public int getRequiredPermissionLevel() { return 2; }
            @Override public void execute(net.minecraft.server.MinecraftServer srv, net.minecraft.command.ICommandSender s, String[] a) {
                glossDebug = a.length > 0 ? Integer.parseInt(a[0]) : 0;
                s.sendMessage(new net.minecraft.util.text.TextComponentString("gloss debug mode " + glossDebug));
            }
        });
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        for (Defs.Def d : Defs.ALL) {
            BlockFurniture b = new BlockFurniture(d);
            BLOCKS.put(d.id, b);
            e.getRegistry().register(b);
        }
        FILLER = new BlockFiller();
        e.getRegistry().register(FILLER);
        GLOSS_TILE = new BlockGlossTile();
        e.getRegistry().register(GLOSS_TILE);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        for (BlockFurniture b : BLOCKS.values()) e.getRegistry().register(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        e.getRegistry().register(new net.minecraft.item.ItemCloth(GLOSS_TILE).setRegistryName(GLOSS_TILE.getRegistryName()));   // 16 colours by meta
        for (String c : Defs.COUNTERS) item(e, new ItemCountertop(c), "countertop_" + c);
        item(e, new ItemTreat(6, 0.7F, false), "toast");
        item(e, new ItemTreat(2, 0.3F, true, new PotionEffect(MobEffects.SPEED, 20 * 180, 1), new PotionEffect(MobEffects.HASTE, 20 * 180)), "coffee");
        item(e, new ItemTreat(6, 0.8F, true, new PotionEffect(MobEffects.REGENERATION, 20 * 10)), "smoothie");
        item(e, new ItemTreat(2, 0.4F, true, new PotionEffect(MobEffects.REGENERATION, 20 * 30), new PotionEffect(MobEffects.ABSORPTION, 20 * 60)), "tea");
        item(e, new Item(), "trash_cube");
    }

    private static void item(RegistryEvent.Register<Item> e, Item i, String id) {
        i.setRegistryName(ID, id).setUnlocalizedName(ID + "." + id).setCreativeTab(TAB);
        ITEMS.put(id, i);
        e.getRegistry().register(i);
    }
}
