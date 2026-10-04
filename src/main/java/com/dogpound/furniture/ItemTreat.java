package com.dogpound.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/** Toast, coffee, smoothies, tea. Drinks are drunk (and count toward your bladder). */
public class ItemTreat extends ItemFood {
    private final boolean drink;
    private final PotionEffect[] effects;

    public ItemTreat(int heal, float sat, boolean drink, PotionEffect... effects) {
        super(heal, sat, false);
        this.drink = drink;
        this.effects = effects;
        if (drink) setAlwaysEdible();
    }

    @Override public EnumAction getItemUseAction(ItemStack s) { return drink ? EnumAction.DRINK : EnumAction.EAT; }

    @Override protected void onFoodEaten(ItemStack s, World w, EntityPlayer p) {
        if (w.isRemote) return;
        for (PotionEffect e : effects) p.addPotionEffect(new PotionEffect(e));
        if (drink && s.getItem() != PrideFurniture.ITEMS.get("smoothie")) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!p.addItemStackToInventory(bottle)) p.dropItem(bottle, false);
        }
    }
}
