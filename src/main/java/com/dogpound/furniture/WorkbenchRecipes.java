package com.dogpound.furniture;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.oredict.OreDictionary;

/** The Furniture Workbench's catalog (assets/pridefurniture/workbench.json, written by tools/gen_assets.py). */
public final class WorkbenchRecipes {
    private WorkbenchRecipes() {}

    public static final class Ing {
        public final Item item; public final int data; public final String ore; public final int count;
        Ing(Item item, int data, String ore, int count) { this.item = item; this.data = data; this.ore = ore; this.count = count; }
        public boolean matches(ItemStack s) {
            if (s.isEmpty()) return false;
            if (ore != null) { for (int id : OreDictionary.getOreIDs(s)) if (OreDictionary.getOreName(id).equals(ore)) return true; return false; }
            return s.getItem() == item && (data < 0 || s.getMetadata() == data);
        }
        /** what to show for this ingredient (ore entries cycle through their options) */
        public ItemStack show(long tick) {
            if (ore != null) {
                NonNullList<ItemStack> all = OreDictionary.getOres(ore);
                if (all.isEmpty()) return ItemStack.EMPTY;
                ItemStack s = all.get((int) (tick / 20 % all.size())).copy();
                if (s.getMetadata() == OreDictionary.WILDCARD_VALUE) s.setItemDamage(0);
                return s;
            }
            return new ItemStack(item, 1, Math.max(0, data));
        }
    }

    public static final class Recipe {
        public final String id, category; public final int count; public final List<Ing> ings;
        Recipe(String id, String category, int count, List<Ing> ings) { this.id = id; this.category = category; this.count = count; this.ings = ings; }
        public ItemStack output() {
            Item i = Item.getByNameOrId(PrideFurniture.ID + ":" + id);
            return i == null ? ItemStack.EMPTY : new ItemStack(i, count);
        }
    }

    private static List<Recipe> all;

    public static List<Recipe> all() {
        if (all != null) return all;
        all = new ArrayList<>();
        try (Reader r = new InputStreamReader(PrideFurniture.class.getResourceAsStream("/assets/pridefurniture/workbench.json"), StandardCharsets.UTF_8)) {
            for (JsonElement e : new JsonParser().parse(r).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                List<Ing> ings = new ArrayList<>();
                for (JsonElement ie : o.getAsJsonArray("ingredients")) {
                    JsonObject i = ie.getAsJsonObject();
                    int n = i.get("count").getAsInt();
                    if (i.has("ore")) ings.add(new Ing(null, -1, i.get("ore").getAsString(), n));
                    else {
                        Item it = Item.REGISTRY.getObject(new ResourceLocation(i.get("item").getAsString()));
                        if (it != null) ings.add(new Ing(it, i.has("data") ? i.get("data").getAsInt() : -1, null, n));
                    }
                }
                all.add(new Recipe(o.get("id").getAsString(), o.get("category").getAsString(), o.get("count").getAsInt(), ings));
            }
        } catch (Exception ex) {
            System.out.println("[PrideFurniture] workbench catalog failed to load: " + ex);
        }
        return all;
    }

    public static Recipe get(String id) { for (Recipe r : all()) if (r.id.equals(id)) return r; return null; }

    public static List<String> categories() {
        Set<String> s = new LinkedHashSet<>();
        for (Recipe r : all()) s.add(r.category);
        return new ArrayList<>(s);
    }

    public static int have(EntityPlayer p, Ing ing) {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory) if (ing.matches(s)) n += s.getCount();
        return n;
    }

    /** how many times this recipe can be made from what's in the player's inventory */
    public static int canMake(EntityPlayer p, Recipe r) {
        if (p.capabilities.isCreativeMode) return 64;
        int max = 64;
        for (Ing i : r.ings) max = Math.min(max, have(p, i) / i.count);
        return max;
    }

    /** server side: take the materials, hand over the furniture */
    public static void craft(EntityPlayer p, String id, int times) {
        Recipe r = get(id);
        if (r == null) return;
        ItemStack out = r.output();
        if (out.isEmpty()) return;
        times = Math.min(times, canMake(p, r));
        for (int t = 0; t < times; t++) {
            if (!p.capabilities.isCreativeMode) for (Ing i : r.ings) {
                int need = i.count;
                for (ItemStack s : p.inventory.mainInventory) {
                    if (need == 0) break;
                    if (!i.matches(s)) continue;
                    int take = Math.min(need, s.getCount());
                    ItemStack cont = s.getItem().getContainerItem(s);       // buckets stay
                    s.shrink(take);
                    need -= take;
                    if (!cont.isEmpty()) { cont.setCount(take); if (!p.addItemStackToInventory(cont)) p.dropItem(cont, false); }
                }
            }
            ItemStack give = out.copy();
            if (!p.addItemStackToInventory(give)) p.dropItem(give, false);
        }
        if (times > 0) p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 0.8F, 1.1F);
        p.inventoryContainer.detectAndSendChanges();
    }

}
